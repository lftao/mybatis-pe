package com.lftao.mybatis.support;

import java.io.Serializable;
import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;

import com.lftao.mybatis.pagination.Page;
import com.lftao.mybatis.utils.ScriptSqlUtils;
import com.lftao.mybatis.utils.SqlCommand;

/**
 * 常用基础方法接口
 * 
 * @author tao
 */
public class DaoImpl<T> implements DaoInterface<T> {
    protected SqlSessionTemplate sqlSessionTemplate;
    private Class<T> classType;

    public DaoImpl() {
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public Class<T> getClassType() {
        return this.classType;
    }

    public void setClassType(Class<T> classType) {
        this.classType = classType;
    }

    @Override
    public int save(T entity) {
        String statementId = ScriptSqlUtils.getStatementId(SqlCommand.INSERT, classType);
        return sqlSessionTemplate.insert(statementId, entity);
    }

    @Override
    public T findById(Serializable id) {
        String statementId = ScriptSqlUtils.getStatementId(SqlCommand.SQL_FIND_BY_ID, classType);
        return sqlSessionTemplate.selectOne(statementId, id);
    }

    @Override
    public int deleteById(Serializable id) {
        String statementId = ScriptSqlUtils.getStatementId(SqlCommand.SQL_DELETE_BY_ID, classType);
        return sqlSessionTemplate.delete(statementId, id);
    }

    @Override
    public int updateById(T entity) {
        String statementId = ScriptSqlUtils.getStatementId(SqlCommand.SQL_UPDATE_BY_ID, classType);
        return sqlSessionTemplate.update(statementId, entity);
    }

    @Override
    public int updateNotNullById(T entity) {
        String statementId = ScriptSqlUtils.getStatementId(SqlCommand.SQL_UPDATE_NOT_NULL_BY_ID, classType);
        return sqlSessionTemplate.update(statementId, entity);
    }

    @Override
    public Page<T> findPage(T qyery) {
        return null;
    }

    @Override
    public List<T>  findByEntity(T query) {
        String statementId = ScriptSqlUtils.getStatementId(SqlCommand.SQL_FIND_BY_ENTITY, classType);
        return sqlSessionTemplate.selectList(statementId, query);
    }
}