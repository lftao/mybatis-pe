package com.lftao.mybatis;

import java.util.Map;

import com.lftao.mybatis.exception.MybatisException;
import com.lftao.mybatis.pagination.PageQuery;
import com.lftao.mybatis.utils.BeanUtils;

public class HolderContext {
    public static final String PAGE_NUM = "pageNum";
    public static final String PAGE_SIZE = "pageSize";
    protected static final ThreadLocal<PageQuery> LOCAL_PAGE = new ThreadLocal<>();

    public static void startPage(int pageNum, int pageSize) {
        PageQuery query = new PageQuery();
        query.setPage(pageNum);
        query.setSize(pageSize);
        LOCAL_PAGE.set(query);
    }

    public static Map<String, Object> wrapperPageData(Object param) {
        PageQuery query = LOCAL_PAGE.get();
        Map<String, Object> map = BeanUtils.toMap(param);
        Object pageNum = map.get(PAGE_NUM);
        Object pageSize = map.get(PAGE_SIZE);
        if (query != null) {
            pageNum = query.getPage();
            pageSize = query.getSize();
        }
        if (pageNum == null || pageSize == null) {
            throw new MybatisException("the page query Map or HanderContext missing pageNum or pageSize");
        }
        Integer numberPageNum = null;
        Integer numberPageSize = null;
        if (pageNum instanceof Integer && pageSize instanceof Integer) {
            numberPageNum = (Integer) pageNum;
            numberPageSize = (Integer) pageSize;
        } else {
            numberPageNum = Integer.valueOf(pageNum.toString());
            numberPageSize = Integer.valueOf(pageSize.toString());
        }
        //SQL-STAR-END
        Integer start = (numberPageNum - 1) * numberPageSize;
        Integer end = numberPageNum * numberPageSize;
        map.put(PAGE_NUM, start);
        map.put(PAGE_SIZE, end);
        return map;
    }

    public static void clear() {
        LOCAL_PAGE.remove();
    }
}
