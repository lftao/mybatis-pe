package com.lftao.mybatis.pagination;

import java.io.Serializable;

/**
 * 简单分页查询
 * 
 * @author tao
 */
public class PageQuery implements Serializable {
    private static final long serialVersionUID = 1922642587258832402L;
    // 当前页
    private Integer page = 1;
    // 每页数量
    private Integer size = 10;

    /**
     * 当前页
     * 
     * @return 当前页
     */
    public Integer getPage() {
        return page;
    }

    /**
     * 当前页
     * 
     * @param page
     *            页数
     */
    public void setPage(Integer page) {
        if (page == null || page <= 0) {
            page = 1;
        }
        this.page = page;
    }

    /**
     * 每页数量
     * 
     * @return 每页数量
     */
    public Integer getSize() {
        if (size == null) {
            size = 10;
        }
        return size;
    }

    /**
     * 每页数量
     * 
     * @param size
     *            每页数量
     */
    public void setSize(Integer size) {
        if (size != null) {
            this.size = size;
        }
    }
}
