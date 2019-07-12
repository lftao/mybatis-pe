package com.lftao.mybatis.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.FactoryBean;

public class MapperBeanPrpxy<T> extends MapperFactoryBean<T> implements FactoryBean<T> {
    private Class<T> mapperInterface;

    public MapperBeanPrpxy() {
        super();
    }

    public MapperBeanPrpxy(Class<T> mapperInterface) {
        super(mapperInterface);
        this.mapperInterface = mapperInterface;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() throws Exception {
        // 代理路由
        RouteMapperProxy<T> mapperProxy = new RouteMapperProxy<T>(super.getObject());
        // 泛型类型
        Type[] genType = mapperInterface.getGenericInterfaces();
        if (genType != null && genType.length > 0) {
            Type[] params = ((ParameterizedType) genType[0]).getActualTypeArguments();
            if (params.length > 0) {
                mapperProxy.setClassType((Class<T>) params[0]);
            }
        }
        mapperProxy.setSqlSessionTemplate(this.getSqlSessionTemplate());
        mapperProxy.setMapperInterface(mapperInterface);
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
    }
}
