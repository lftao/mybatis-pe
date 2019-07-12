package com.lftao.mybatis.exception;

/**
 * 异常信息
 * 
 * @author tao
 */
public class MybatisException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MybatisException(String message) {
        super(message);
    }

    public MybatisException(Throwable throwable) {
        super(throwable);
    }

    public MybatisException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
