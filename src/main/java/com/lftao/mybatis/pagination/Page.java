package com.lftao.mybatis.pagination;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页实体
 * 
 * @author tao
 */
public class Page<T> extends PageQuery {
    private static final long serialVersionUID = -3011180843826014135L;
    private Long total = 0L;// 总数
    private List<T> rows = new ArrayList<>();

    /**
     * 总数
     * 
     * @return 总数
     */
    public Long getTotal() {
        return total;
    }

    /**
     * 总数
     * 
     * @param total
     *            总数
     */
    public void setTotal(Long total) {
        this.total = total;
    }

    /**
     * 结果集
     * 
     * @return 结果集合
     */
    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }
}
