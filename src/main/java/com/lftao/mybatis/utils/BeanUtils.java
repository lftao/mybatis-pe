package com.lftao.mybatis.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * BeanUtils Set get
 * 
 * @author TLF
 */
public class BeanUtils {
    private final static String EMPTY = "";
    private final static String SPLIT = "_";
    private final static Pattern numberPattern = Pattern.compile("^[-\\+]?[\\d]+$");
    private final static Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    /**
     * 初始化加载 clazz-field
     * 
     * @param clazz
     *            class
     * @return
     */
    public static List<Field> getAllFieldsCache(Class<?> clazz) {
        List<Field> allFields = fieldCache.get(clazz);
        if (allFields == null) {
            allFields = getAllFields(clazz);
            fieldCache.put(clazz, allFields);
        }
        return allFields;
    }

    /**
     * 获得所有 Field 包含父类
     * 
     * @param clazz
     *            class
     * @return field 集合
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> allField = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            allField.add(field);
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            ClassLoader classLoader = superclass.getClassLoader();
            if (classLoader != null) {
                if (superclass.getDeclaredFields().length > 0) {
                    allField.addAll(getAllFields(superclass));
                }
            }
        }
        return allField;
    }

    /**
     * 首字母大写 <br>
     * user - User
     * 
     * @param column
     *            字符
     * @return 结果
     */
    public static String firstToHump(String column) {
        if (isBlank(column)) {
            return EMPTY;
        } else {
            return column.substring(0, 1).toUpperCase() + column.substring(1);
        }
    }

    /**
     * 首字母小写
     * 
     * @param column
     *            字符
     * @return 结果
     */
    public static String firstToMix(String column) {
        if (isBlank(column)) {
            return EMPTY;
        } else {
            return column.substring(0, 1).toLowerCase() + column.substring(1);
        }
    }

    /**
     * 字段转驼峰命名 <br>
     * user_name - userName
     * 
     * @param column
     *            字符
     * @return 结果
     */
    public static String columnToHump(String column) {
        if (isBlank(column)) {
            return EMPTY;
        } else if (column.indexOf(SPLIT) < 0) {
            return column.substring(0, 1).toLowerCase() + column.substring(1);
        } else {
            StringBuilder result = new StringBuilder();
            String[] columns = column.split(SPLIT);
            for (String columnSplit : columns) {
                if (columnSplit.isEmpty()) {
                    continue;
                }
                if (result.length() == 0) {
                    result.append(columnSplit.toLowerCase());
                } else {
                    result.append(columnSplit.substring(0, 1).toUpperCase()).append(columnSplit.substring(1).toLowerCase());
                }
            }
            return result.toString();
        }
    }

    /**
     * 字段转驼峰命名 <br>
     * userName - user_name
     * 
     * @param column
     *            字符
     * @return 结果
     */
    public static String columnToHumpReversal(String column) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < column.length(); i++) {
            char charAt = column.charAt(i);
            if (Character.isUpperCase(charAt)) {
                result.append("_");
                charAt = Character.toLowerCase(charAt);
            }
            result.append(charAt);
        }
        return result.toString();
    }

    /**
     * 判断段是否是数字
     * 
     * @param str
     *            字符
     * @return true/false
     */
    public static boolean isNumber(String str) {
        return numberPattern.matcher(str).matches();
    }

    /**
     * 判断的包装类
     * 
     * @param clz
     *            class
     * @return true/false
     */
    public static boolean isWrapClass(Class<?> clz) {
        try {
            return ((Class<?>) clz.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断的是否为空
     * 
     * @param s
     *            字符串
     * @return 是否为空
     */
    public static boolean isBlank(String s) {
        if (s == null || EMPTY.equals(s.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 将Object对象里面的属性和值转化成Map对象
     *
     * @param obj
     *            对象
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return new HashMap<String, Object>();
        }
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : getAllFieldsCache(obj.getClass())) {
            try {
                String fieldName = field.getName();
                Object value = field.get(obj);
                if (value != null) {
                    map.put(fieldName, value);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }
}
