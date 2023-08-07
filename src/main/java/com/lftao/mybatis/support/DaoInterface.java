package com.lftao.mybatis.support;

import java.io.Serializable;
import java.util.List;

import com.lftao.mybatis.pagination.Page;

/**
 * 常用基础方法接口
 * 
 * @author tao
 */
public interface DaoInterface<T> {
    /**
     * 持久化实体
     * 
     * @param entity
     *            实体
     * @return 变更值
     */
    int save(T entity);

    /**
     * 根据id查找
     * 
     * @param id
     *            标识符
     * @return 实体
     */
    T findById(Serializable id);

    /**
     * 根据id查找
     * 
     * @param query
     *            搜索非空字段
     * @return 实体
     */
    List<T> findByEntity(T query);

    /**
     * 根据id删除
     * 
     * @param id
     *            标识符
     * @return 变更值
     */
    int deleteById(Serializable id);

    /**
     * 根据id更新
     * 
     * @param entity
     *            实体
     * @return 变更值
     */
    int updateById(T entity);

    /**
     * 更新非空字段
     * 
     * @param entity
     *            实体
     * @return 变更值
     */
    int updateNotNullById(T entity);

    /**
     * 执行分页查询
     * 
     * @param qyery
     *            没空查询
     * @return 分页数据
     */
    Page<T> findPage(T qyery);
    
    Class<T> getClassType();
    
}
