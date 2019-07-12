package com.lftao.mybatis.support;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.MapperMethod.MethodSignature;
import org.apache.ibatis.binding.MapperProxy;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.scripting.xmltags.StaticTextSqlNode;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import com.lftao.mybatis.HolderContext;
import com.lftao.mybatis.pagination.Page;
import com.lftao.mybatis.utils.ScriptSqlUtils;
import com.lftao.mybatis.utils.SqlCommand;

/**
 * 代理路由转发
 * 
 * @author TLF
 * @param <T>
 */
public class RouteMapperProxy<T> extends DaoImpl<T> implements InvocationHandler, Serializable {
    private static final long serialVersionUID = -7931253586322777558L;
    private MapperProxy<T> bean;
    private Class<?> mapperInterface;

    /**
     * 构造方法路由
     * 
     * @param obj
     */
    @SuppressWarnings("unchecked")
    public RouteMapperProxy(Object obj) {
        super.setSqlSessionTemplate(sqlSessionTemplate);
        // JDK代理模式
        if (obj instanceof java.lang.reflect.Proxy) {
            this.bean = (MapperProxy<T>) java.lang.reflect.Proxy.getInvocationHandler(obj);
        }
        // cglib代理模式
        if (obj instanceof org.springframework.cglib.proxy.Proxy) {
            this.bean = (MapperProxy<T>) org.springframework.cglib.proxy.Proxy.getInvocationHandler(obj);
        }
    }

    public Class<?> getMapperInterface() {
        return this.mapperInterface;
    }

    public void setMapperInterface(Class<?> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> returnType = method.getReturnType();
        try {
            if (returnType != null && returnType.isAssignableFrom(Page.class) && returnType.getSuperclass() != null) {
                // 包装分页
                return wraperPage(method, args);
            }
            // 默认方法
            if (method.getDeclaringClass().isAssignableFrom(DaoInterface.class)) {
                return method.invoke(this, args);
            }
            // 原始方法
            return bean.invoke(proxy, method, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } finally {
            HolderContext.clear();
        }
    }

    /**
     * 包装分页查询
     * 
     * @param method
     *            方法
     * @param args
     *            参数
     * @return 分页结果
     */
    @SuppressWarnings({ "unchecked" })
    private Page<T> wraperPage(Method method, Object[] args) {
        Configuration config = sqlSessionTemplate.getConfiguration();
        config.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.WARNING);
        //
        MethodSignature signature = new MethodSignature(config, mapperInterface, method);
        Object param = signature.convertArgsToSqlCommandParam(args);
        // 添加分页参数
        Map<String, Object> pageQuery = HolderContext.wrapperPageData(param);
        try {
            MappedStatement mappedStatement = null;
            if (method.getDeclaringClass().isAssignableFrom(DaoInterface.class)) {
                mappedStatement = config.getMappedStatement(ScriptSqlUtils.getStatementId(SqlCommand.SQL_FIND_PAGE_BY_ENTITY, getClassType()));
            } else {
                mappedStatement = config.getMappedStatement(method.getName());
            }
            String id = mappedStatement.getId();
            String pageKey = "@page";
            String pageId = id + pageKey;
            // 记录动态SQL脚本
            List<SqlNode> newSqlNodes = null;
            // 判断是否注册分页
            if (!config.hasStatement(pageId)) {
                // 通过反射获取原始动态SQL - DynamicSqlSource、RawSqlSource
                SqlSource source = mappedStatement.getSqlSource();
                SqlSource sqlSource = null;
                // 静态SQL
                if (source instanceof RawSqlSource) {
                    Field fieldSqlSource = source.getClass().getDeclaredField("sqlSource");
                    fieldSqlSource.setAccessible(true);
                    Object object = fieldSqlSource.get(source);
                    // 静态SQL
                    if (object instanceof StaticSqlSource) {
                        Field fieldSql = object.getClass().getDeclaredField("sql");
                        fieldSql.setAccessible(true);
                        String sql = fieldSql.get(object).toString();
                        String newSql = "select * from (" + sql + ") _tb limit ?,?";
                        sqlSource = new StaticSqlSource(config, newSql);
                        newSqlNodes = new ArrayList<>(3);
                        newSqlNodes.add(new StaticTextSqlNode(null));
                        newSqlNodes.add(new StaticTextSqlNode(sql));
                        newSqlNodes.add(new StaticTextSqlNode(null));
                    }
                }
                // 动态SQL
                if (source instanceof DynamicSqlSource) {
                    Field field = source.getClass().getDeclaredField("rootSqlNode");
                    field.setAccessible(true);
                    MixedSqlNode mixedSqlNode = (MixedSqlNode) field.get(source);
                    Field fieldContents = mixedSqlNode.getClass().getDeclaredField("contents");
                    fieldContents.setAccessible(true);
                    List<SqlNode> sqlNodes = (List<SqlNode>) fieldContents.get(mixedSqlNode);
                    // 动态包装分页SQL
                    newSqlNodes = new ArrayList<>(sqlNodes);
                    newSqlNodes.add(0, new StaticTextSqlNode("select * from ("));
                    newSqlNodes.add(new StaticTextSqlNode(") _tb limit #{" + HolderContext.PAGE_NUM + "},#{" + HolderContext.PAGE_SIZE + "}"));
                    sqlSource = new DynamicSqlSource(config, new MixedSqlNode(newSqlNodes));
                }
                // ----
                ParameterMap parameterMap = mappedStatement.getParameterMap();
                List<ParameterMapping> parameterMappings = parameterMap.getParameterMappings();
                ArrayList<ParameterMapping> pms = new ArrayList<>(parameterMappings);
                pms.add(new ParameterMapping.Builder(config, HolderContext.PAGE_NUM, Integer.class).build());
                pms.add(new ParameterMapping.Builder(config, HolderContext.PAGE_SIZE, Integer.class).build());
                Builder builder = new Builder(config, pageId, sqlSource, SqlCommandType.SELECT);
                // 结果集映射
                builder.resultMaps(mappedStatement.getResultMaps());
                // 执行参数
                builder.parameterMap(new ParameterMap.Builder(config, null, Map.class, pms).build());
                MappedStatement mappedStatementNew = builder.build();
                // 添加配置映射
                config.addMappedStatement(mappedStatementNew);
            }
            // 执行查询
            List<T> rows = sqlSessionTemplate.selectList(pageId, pageQuery);
            Page<T> page = new Page<>();
            page.setRows(rows);
            // 分页查询
            countPage(page, config, mappedStatement, param, newSqlNodes);
            return page;
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error count query  Cause: " + e, e);
        }
    }

    /**
     * 包装分页条数
     * 
     * @param page
     *            分页对象
     * @param config
     *            myBatis 配置
     * @param method
     *            执行方法
     * @param param
     *            参数
     */
    private void countPage(Page<T> page, Configuration config, MappedStatement mappedStatement, Object param, List<SqlNode> newSqlNodes) {
        try {
            String id = mappedStatement.getId();
            String countKey = "@count";
            String countId = id + countKey;
            // 判断是否注册分页
            if (!config.hasStatement(countId)) {
                newSqlNodes = new ArrayList<>(newSqlNodes);
                newSqlNodes.remove(0);
                newSqlNodes.remove(newSqlNodes.size() - 1);
                newSqlNodes.add(0, new StaticTextSqlNode("select count(*) num from ("));
                newSqlNodes.add(new StaticTextSqlNode(")_tb"));
                // -
                DynamicSqlSource sqlSource = new DynamicSqlSource(config, new MixedSqlNode(newSqlNodes));
                Builder builder = new Builder(config, countId, sqlSource, SqlCommandType.SELECT);
                // 结果集映射
                List<ResultMapping> resultMappings = new ArrayList<>(1);
                resultMappings.add(new ResultMapping.Builder(config, null, "num", Long.class).build());
                // 结果集映射
                List<ResultMap> resultMaps = new ArrayList<>(1);
                resultMaps.add(new ResultMap.Builder(config, countId, Long.class, resultMappings).build());
                builder.resultMaps(resultMaps);
                // 执行参数
                builder.parameterMap(mappedStatement.getParameterMap());
                MappedStatement mappedStatementNew = builder.build();
                // 添加配置映射
                config.addMappedStatement(mappedStatementNew);
            }
            // 执行查询
            sqlSessionTemplate.select(countId, param, new ResultHandler<Long>() {
                public void handleResult(ResultContext<? extends Long> resultContext) {
                    page.setTotal(resultContext.getResultObject());
                }
            });
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error count query  Cause: " + e, e);
        }
    }
}
