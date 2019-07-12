package com.lftao.mybatis.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * TableMapping
 * 
 * @author tao
 */
public class TableMapping implements Serializable {
    private static final long serialVersionUID = 9134744409102951517L;
    private static final Map<Class<?>, TableMapping> mapping = new HashMap<>();
    private Class<?> classz;
    private String keyId;
    private String tableName;
    private String keyColumn;
    private LinkedCaseInsensitiveMap<String> columns;
    private LinkedCaseInsensitiveMap<String> properties;

    public Class<?> getClassz() {
        return this.classz;
    }

    public void setClassz(Class<?> classz) {
        this.classz = classz;
    }

    public String getKeyId() {
        return this.keyId;
    }

    public TableMapping setKeyId(String keyId) {
        this.keyId = keyId;
        return this;
    }

    public String getKeyColumn() {
        return this.keyColumn;
    }

    public TableMapping setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
        return this;
    }

    public String getTableName() {
        return this.tableName;
    }

    public TableMapping setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public LinkedCaseInsensitiveMap<String> getColumns() {
        return this.columns;
    }

    public TableMapping setColumns(LinkedCaseInsensitiveMap<String> columns) {
        this.columns = columns;
        return this;
    }

    public LinkedCaseInsensitiveMap<String> getProperties() {
        return this.properties;
    }

    public TableMapping setProperties(LinkedCaseInsensitiveMap<String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * 获取-Column
     * 
     * @param propertie
     * @return Column
     */
    public String getColumnByPropertie(String propertie) {
        String column = properties.get(propertie);
        if (column == null) {
            column = BeanUtils.columnToHumpReversal(propertie);
        }
        return column;
    }

    /**
     * 获取-Column to propertie
     * 
     * @param Column
     * @return propertie
     */
    public String getPropertieByColumn(String column) {
        String propertie = columns.get(column);
        if (propertie == null) {
            propertie = BeanUtils.columnToHumpReversal(column);
        }
        return propertie;
    }

    /**
     * 获取的所有字段
     * 
     * @return allFields
     */
    public List<Field> getAllFields() {
        return BeanUtils.getAllFieldsCache(classz);
    }

    /**
     * 指定字段Field
     * 
     * @param propertie
     *            字段
     * @return
     */
    public Field getField(String propertie) {
        List<Field> list = getAllFields();
        return list.stream().filter(f -> f.getName().equals(propertie)).findFirst().get();
    }

    /**
     * 获取tableMapping
     * 
     * @param key
     *            包路径
     * @return 结果
     */
    public static TableMapping getMapping(Class<?> key) {
        TableMapping tableMapping = mapping.get(key);
        if (tableMapping == null) {
            throw new RuntimeException(key.getName() + " tableMapping is not found");
        }
        return tableMapping;
    }

    /**
     * add tableMapping
     * 
     * @param key
     *            包路径
     * @param TableMapping
     *            tableMapping
     */
    public static void addMapping(Class<?> key, TableMapping tableMapping) {
        mapping.put(key, tableMapping);
    }
}
