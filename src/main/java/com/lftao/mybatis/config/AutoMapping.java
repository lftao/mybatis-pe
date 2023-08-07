package com.lftao.mybatis.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.LinkedCaseInsensitiveMap;

import com.lftao.mybatis.annotations.Column;
import com.lftao.mybatis.annotations.DaoMapperScan;
import com.lftao.mybatis.annotations.Key;
import com.lftao.mybatis.annotations.Table;
import com.lftao.mybatis.annotations.Transient;
import com.lftao.mybatis.support.MapperBeanProxy;
import com.lftao.mybatis.utils.BeanUtils;
import com.lftao.mybatis.utils.ScriptSqlUtils;
import com.lftao.mybatis.utils.SqlCommand;
import com.lftao.mybatis.utils.TableMapping;

public class AutoMapping implements ImportBeanDefinitionRegistrar {
	private static final Logger LOGGER = LoggerFactory.getLogger(AutoMapping.class);
	private static final String NODE_PATH_PROPERTY = "property";
	private static final String NODE_PATH_TRANSIENT = "transient";
	private static final String NODE_TABLE_NAME = "table";
	private static final String NODE_TABLE_ID = "id";
	private static final String NODE_COLUMN = "column";
	private static final String NODE_NAME = "name";
	private Configuration configuration;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		try {
			Class<?> claszApp = Class.forName(importingClassMetadata.toString());
			String scanPackage = claszApp.getPackage().getName();
			DaoMapperScan daoMapperScan = claszApp.getAnnotation(DaoMapperScan.class);
			if (daoMapperScan != null) {
				scanPackage = daoMapperScan.value();
			}
			BeanDefinitionBuilder mapperScanner = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
			mapperScanner.addPropertyValue("processPropertyPlaceHolders", true);
			mapperScanner.addPropertyValue("annotationClass", Mapper.class);
			mapperScanner.addPropertyValue("basePackage", scanPackage);
			mapperScanner.addPropertyValue("mapperFactoryBeanClass", MapperBeanProxy.class);
			// BeanWrapper beanWrapper = new BeanWrapperImpl(MapperScannerConfigurer.class);
			registry.registerBeanDefinition(MapperScannerConfigurer.class.getName(), mapperScanner.getBeanDefinition());

			// -
			BeanDefinitionBuilder lastInitBean = BeanDefinitionBuilder.genericBeanDefinition(LastInitBean.class);
			lastInitBean.addPropertyValue(AutoMapping.class.getSimpleName(), this);
			registry.registerBeanDefinition(LastInitBean.class.getName(), lastInitBean.getBeanDefinition());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		// doMappingTable(new Resource[] {new ClassPathResource("/mapper/Table.xml")});
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 读取映射关系
	 */
	public void doMappingTable(Class<?> classz) {
		List<Field> fields = BeanUtils.getAllFieldsCache(classz);
		TableMapping tableMapping = new TableMapping();
		// DB字段
		LinkedCaseInsensitiveMap<String> columns = new LinkedCaseInsensitiveMap<>(fields.size());
		// 属性字段
		LinkedCaseInsensitiveMap<String> properties = new LinkedCaseInsensitiveMap<>(fields.size());
		// 瞬时字段
		Set<String> transients = new HashSet<>();
		for (Field field : fields) {
			String property = field.getName();
			// 排除静态
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (property.contains("CGLIB")) {
				continue;
			}
			// 跳过
			Transient isTransient1 = field.getAnnotation(Transient.class);
			java.beans.Transient isTransient2 = field.getAnnotation(java.beans.Transient.class);
			if (isTransient1 != null || isTransient2 != null) {
				transients.add(property);
				continue;
			}
			// 主键
			Key isKey = field.getAnnotation(Key.class);
			if (isKey != null) {
				tableMapping.setKeyId(property);
				String keyValue = isKey.value();
				if (keyValue.length() == 0) {
					keyValue = property;
				}
				tableMapping.setKeyColumn(keyValue);
			}
			Column columnAno = field.getAnnotation(Column.class);
			String column = property;
			if (columnAno != null) {
				column = columnAno.value();
			}
			columns.put(column, property);
			properties.put(property, column);
		}

		// 表明
		Table tableName = classz.getAnnotation(Table.class);
		if (tableName != null) {
			tableMapping.setTableName(tableName.value());
		} else {
			tableMapping.setTableName(classz.getSimpleName());
		}
		tableMapping.setColumns(columns);
		tableMapping.setProperties(properties);
		tableMapping.setTransientProperties(transients);
		// 记录类映射
		tableMapping.setClassz(classz);
		TableMapping.addMapping(tableMapping.getClassz(), tableMapping);
		// 构造XML映射对象
		buildMappedStatement(tableMapping);
	}

	/**
	 * 读取映射关系
	 */
	public void doMappingTable(Resource[] tableMappingXml) {
		// 遍历配置XML
		for (Resource resource : tableMappingXml) {
			try {
				XPathParser xPathParser = new XPathParser(resource.getInputStream());
				List<XNode> xNodes = xPathParser.evalNodes("/mapper/class");
				for (XNode xNode : xNodes) {
					String type = xNode.getStringAttribute(NODE_NAME);
					if (type == null || type.trim().length() == 0) {
						throw new RuntimeException("table name is missing");
					}
					Class<?> classz = Class.forName(type);
					TableMapping tableMapping = new TableMapping();
					// - 表名字
					String tableName = xNode.getStringAttribute(NODE_TABLE_NAME);
					if (tableName == null) {
						tableName = classz.getName();
					}
					tableMapping.setTableName(tableName);
					// 主键
					XNode nodeId = xNode.evalNode(NODE_TABLE_ID);
					if (nodeId != null) {
						tableMapping.setKeyId(nodeId.getStringAttribute(NODE_PATH_PROPERTY));
						tableMapping.setKeyColumn(nodeId.getStringAttribute(NODE_COLUMN));
					}
					// 瞬时字段
					Set<String> transients = new HashSet<>();
					List<XNode> propsTransient = xNode.evalNodes(NODE_PATH_TRANSIENT);
					for (XNode xNodeTras : propsTransient) {
						String property = xNodeTras.getStringAttribute(NODE_NAME);
						if (property == null) {
							continue;
						}
						transients.add(property);
					}
					// 字段
					List<XNode> columnNodes = xNode.evalNodes(NODE_PATH_PROPERTY);
					LinkedCaseInsensitiveMap<String> columns = new LinkedCaseInsensitiveMap<>(columnNodes.size());
					LinkedCaseInsensitiveMap<String> properties = new LinkedCaseInsensitiveMap<>(columnNodes.size());
					for (XNode node : columnNodes) {
						if (node != null) {
							String property = node.getStringAttribute(NODE_NAME);
							String column = node.getStringAttribute(NODE_COLUMN);
							if (column == null || property == null) {
								LOGGER.debug("column:{},property:{}", property, column);
								continue;
							}
							if (transients.contains(property)) {
								LOGGER.debug("property:{} is transient");
								continue;
							}
							columns.put(column, property);
							properties.put(property, column);
						}
					}
					tableMapping.setColumns(columns);
					tableMapping.setProperties(properties);
					tableMapping.setTransientProperties(transients);
					// 记录类映射
					tableMapping.setClassz(Class.forName(type));
					TableMapping.addMapping(tableMapping.getClassz(), tableMapping);
					// 构造XML映射对象
					buildMappedStatement(tableMapping);
				}
			} catch (Exception e) {
				throw ExceptionFactory.wrapException("Error count query  Cause: " + e, e);
			}
		}
	}

	/**
	 * 构造映射对象
	 * 
	 * @param tableMapping -对象信息
	 */
	private void buildMappedStatement(TableMapping tableMapping) {
		SqlCommand[] values = SqlCommand.values();
		for (SqlCommand command : values) {
			if (SqlCommand.INSERT.equals(command)) {
				addMappedStatement(buildInsert(command, tableMapping));
			}
			// 根据ID查找
			else if (SqlCommand.SQL_FIND_BY_ID.equals(command)) {
				addMappedStatement(buildSelect(command, tableMapping));
			}
			// 分根据对象查询
			else if (SqlCommand.SQL_FIND_BY_ENTITY.equals(command)) {
				addMappedStatement(buildSelect(command, tableMapping));
			}
			// 根据ID删除
			else if (SqlCommand.SQL_DELETE_BY_ID.equals(command)) {
				addMappedStatement(buildDeleteById(command, tableMapping));
			}
			// 根据ID更新
			else if (SqlCommand.SQL_UPDATE_BY_ID.equals(command)) {
				addMappedStatement(buildUpdateById(command, tableMapping));
			}
			// 根据ID更新非空字段
			else if (SqlCommand.SQL_UPDATE_NOT_NULL_BY_ID.equals(command)) {
				addMappedStatement(buildUpdateById(command, tableMapping));
			}
			// 根据实体更新非空字段
			else if (SqlCommand.SQL_UPDATE_NOT_NULL_BY_ENTITY.equals(command)) {
				addMappedStatement(buildUpdateByEntity(command, tableMapping));
			}
			// 分页查询
			else if (SqlCommand.SQL_FIND_PAGE_BY_ENTITY.equals(command)) {
				addMappedStatement(buildSelect(command, tableMapping));
			}
		}
	}

	/**
	 * 添加配置
	 * 
	 * @param MappedStatement - 对象信息
	 */
	private void addMappedStatement(MappedStatement ms) {
		if (ms == null) {
			return;
		}
		configuration.addMappedStatement(ms);
	}

	/**
	 * 构造SQL脚本
	 * 
	 * @param command      -SqlCommand
	 * @param tableMapping -对象信息
	 * @return SqlSource
	 */
	private SqlSource getSqlSource(SqlCommand command, TableMapping tableMapping) {
		Class<?> classz = tableMapping.getClassz();
		// 构建SQL
		String sql = ScriptSqlUtils.getScriptSql(command, classz);
		LOGGER.info("{}|{}|{}", classz.getName(), command.getCommand(), sql);
		LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
		return languageDriver.createSqlSource(configuration, sql, tableMapping.getClassz());
	}

	/**
	 * 构造插入脚本
	 * 
	 * @param command      -SqlCommand
	 * @param tableMapping -对象信息
	 * @return MappedStatement
	 */
	private MappedStatement buildInsert(SqlCommand command, TableMapping tableMapping) {
		Class<?> classz = tableMapping.getClassz();
		String statementId = ScriptSqlUtils.getStatementId(command, classz);
		Field fieldId = tableMapping.getField(tableMapping.getKeyId());
		if(fieldId == null){
			return null;
		}
		SqlSource sqlSource = getSqlSource(command, tableMapping);
		// 主键生成 - TODO 其他策略
		KeyGenerator keyGenerator = new Jdbc3KeyGenerator();
		return new MappedStatement.Builder(configuration, statementId, sqlSource, SqlCommandType.INSERT)
				.parameterMap(new ParameterMap.Builder(configuration, classz.getName(), tableMapping.getClassz(), new ArrayList<>()).build())
				.keyGenerator(keyGenerator).keyProperty(tableMapping.getKeyId()).build();
	}

	/**
	 * 构建查询脚本
	 * 
	 * @param command      -SqlCommand
	 * @param tableMapping -对象信息
	 * @return MappedStatement
	 */
	private MappedStatement buildSelect(SqlCommand command, TableMapping tableMapping) {
		Class<?> classz = tableMapping.getClassz();
		String statementId = ScriptSqlUtils.getStatementId(command, classz);
		Field fieldId = tableMapping.getField(tableMapping.getKeyId());
		if(fieldId == null){
			return null;
		}
		SqlSource sqlSource = getSqlSource(command, tableMapping);
		List<ResultMap> resultMaps = new ArrayList<>(1);
		// 全局resultMap
		String resultMapEntityName = classz.getName();
		// 判断系统是否已存在
		Collection<String> hasResultMapNames = configuration.getResultMapNames();
		if (hasResultMapNames.contains(resultMapEntityName)) {
			// 获取已存在
			resultMaps.add(configuration.getResultMap(resultMapEntityName));
		} else {
			// 包装新的resultMap
			// XML > ResultMap
			List<ResultMapping> resultMappings = new ArrayList<>(1);
			LinkedCaseInsensitiveMap<String> columns = tableMapping.getColumns();
			for (String column : columns.keySet()) {
				String propertie = tableMapping.getPropertieByColumn(column);
				Field field = tableMapping.getField(propertie);
				resultMappings.add(new ResultMapping.Builder(configuration, propertie, column, field.getType()).build());
			}
			ResultMap resultMapEntity = new ResultMap.Builder(configuration, resultMapEntityName, classz, resultMappings).build();
			resultMaps.add(resultMapEntity);
			// 放入全局
			configuration.addResultMap(resultMapEntity);
		}
		return new MappedStatement.Builder(configuration, statementId, sqlSource, SqlCommandType.SELECT).resultMaps(resultMaps)
				.parameterMap(new ParameterMap.Builder(configuration, classz.getName(), fieldId.getType(), new ArrayList<>()).build()).build();
	}

	/**
	 * 构建删除脚本
	 * 
	 * @param command      -SqlCommand
	 * @param tableMapping -对象信息
	 * @return MappedStatement
	 */
	private MappedStatement buildDeleteById(SqlCommand command, TableMapping tableMapping) {
		Class<?> classz = tableMapping.getClassz();
		Field fieldId = tableMapping.getField(tableMapping.getKeyId());
		if(fieldId == null){
			return null;
		}
		String statementId = ScriptSqlUtils.getStatementId(command, classz);
		SqlSource sqlSource = getSqlSource(command, tableMapping);
		return new MappedStatement.Builder(configuration, statementId, sqlSource, SqlCommandType.DELETE)
				.parameterMap(new ParameterMap.Builder(configuration, classz.getName(), fieldId.getType(), new ArrayList<>()).build()).build();
	}

	/**
	 * 构建 更新字段脚本
	 * 
	 * @param command      -SqlCommand
	 * @param tableMapping 对象信息
	 * @return MappedStatement
	 */
	private MappedStatement buildUpdateById(SqlCommand command, TableMapping tableMapping) {
		Class<?> classz = tableMapping.getClassz();
		String statementId = ScriptSqlUtils.getStatementId(command, classz);
		Field fieldId = tableMapping.getField(tableMapping.getKeyId());
		if(fieldId == null){
			return null;
		}
		SqlSource sqlSource = getSqlSource(command, tableMapping);
		return new MappedStatement.Builder(configuration, statementId, sqlSource, SqlCommandType.UPDATE)
				.parameterMap(new ParameterMap.Builder(configuration, classz.getName(), tableMapping.getClassz(), new ArrayList<>()).build()).build();
	}
	
	/**
	 * 构建 更新字段脚本
	 * 
	 * @param command      -SqlCommand
	 * @param tableMapping 对象信息
	 * @return MappedStatement
	 */
	private MappedStatement buildUpdateByEntity(SqlCommand command, TableMapping tableMapping) {
		Class<?> classz = tableMapping.getClassz();
		String statementId = ScriptSqlUtils.getStatementId(command, classz);
		SqlSource sqlSource = getSqlSource(command, tableMapping);
		return new MappedStatement.Builder(configuration, statementId, sqlSource, SqlCommandType.UPDATE)
				.parameterMap(new ParameterMap.Builder(configuration, classz.getName(), HashMap.class, new ArrayList<>()).build()).build();
	}

}
