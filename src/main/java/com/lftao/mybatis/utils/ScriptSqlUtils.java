package com.lftao.mybatis.utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * 脚本SQL工具类
 * @author tao
 *
 */
public class ScriptSqlUtils {
    public static String getStatementId(SqlCommand command, Class<?> classz) {
        return classz.getName() + "." + command.getCommand();
    }

    public static String getScriptSql(SqlCommand command, Class<?> classz) {
        TableMapping mapping = TableMapping.getMapping(classz);
        Set<String> transientProperties = mapping.getTransientProperties();
        List<Field> fields = mapping.getAllFields();
        StringBuilder columns = new StringBuilder();
        StringBuilder params = new StringBuilder();
        fields.stream().forEach((field) -> {
            String name = field.getName();
            String column = mapping.getColumnByPropertie(name);
            if (column == null || transientProperties.contains(name)) {
                return;
            }
            // 插入语句
            if (SqlCommand.INSERT.equals(command)) {
                // 所有column字段
                columns.append(String.format(SqlCommand.IF, name, column + ","));
                params.append(String.format(SqlCommand.IF, name, "#{" + name + "},"));
            }
            // 根据ID查询
            else if (SqlCommand.SQL_FIND_BY_ID.equals(command)) {
                // 所有column字段
                columns.append(column + ",");
            }
            // 根据非空参数查询
            else if (SqlCommand.SQL_FIND_BY_ENTITY.equals(command)) {
                // 所有column字段
                columns.append(column + ",");
                params.append(String.format(SqlCommand.IF, name, " and " + column + "=#{" + name + "}"));
            }
            // 分页查询sql同上
            else if (SqlCommand.SQL_FIND_PAGE_BY_ENTITY.equals(command)) {
                // 所有column字段
                columns.append(column + ",");
                params.append(String.format(SqlCommand.IF, name, " and " + column + "=#{" + name + "}"));
            }
            // 根据ID更新
            else if (SqlCommand.SQL_UPDATE_BY_ID.equals(command)) {
                // 所有column字段
                columns.append(column + "=#{" + name + "},");
            }
            // 更新非空字段
            else if (SqlCommand.SQL_UPDATE_NOT_NULL_BY_ID.equals(command)) {
                // 所有column字段
                columns.append(String.format(SqlCommand.IF, name, column + "=#{" + name + "},"));
            }
        });
        // --
        String tableName = mapping.getTableName();
        if (SqlCommand.INSERT.equals(command)) {
            return String.format(command.getScript(), tableName, columns.toString(), params.toString());
        }
        // 根据ID查询
        else if (SqlCommand.SQL_FIND_BY_ID.equals(command)) {
            String keyColumn = mapping.getKeyColumn();
            String keyId = mapping.getKeyId();
            return String.format(command.getScript(), columns.toString(), tableName, keyColumn, keyId);
        }
        // 根据实体参数查询
        else if (SqlCommand.SQL_FIND_BY_ENTITY.equals(command)) {
            return String.format(command.getScript(), columns.toString(), tableName, params);
        }
        // 分页
        else if (SqlCommand.SQL_FIND_PAGE_BY_ENTITY.equals(command)) {
            return String.format(command.getScript(), columns.toString(), tableName, params);
        }
        // 根据ID删除
        else if (SqlCommand.SQL_DELETE_BY_ID.equals(command)) {
            String keyColumn = mapping.getKeyColumn();
            String keyId = mapping.getKeyId();
            return String.format(command.getScript(), tableName, keyColumn, keyId);
        }
        // 根据ID更新
        else if (SqlCommand.SQL_UPDATE_BY_ID.equals(command)) {
            String keyColumn = mapping.getKeyColumn();
            String keyId = mapping.getKeyId();
            return String.format(command.getScript(), tableName, columns.toString(), keyColumn, keyId);
        }
        // 更新非空
        else if (SqlCommand.SQL_UPDATE_NOT_NULL_BY_ID.equals(command)) {
            String keyColumn = mapping.getKeyColumn();
            String keyId = mapping.getKeyId();
            return String.format(command.getScript(), tableName, columns.toString(), keyColumn, keyId);
        }
        return null;
    }
}
