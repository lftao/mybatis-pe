package com.lftao.mybatis.utils;

public enum SqlCommand {
    /**
     * insert into %s values (%s)
     */
    INSERT("insert", "<script>insert into %s(<trim suffixOverrides=','>%s</trim>)values(<trim suffixOverrides=','>%s</trim>)</script>"),
    /**
     * select %s where %s
     */
    SQL_FIND_BY_ID("findById", "<script>select <trim suffixOverrides=','>%s</trim> from %s where %s=#{%s} </script>"),
    /**
     * select %s where %s
     */
    SQL_FIND_BY_ENTITY("findByEntity", "<script>select <trim suffixOverrides=','>%s</trim> from %s <where>%s</where></script>"),
    /**
     * select %s where %s
     */
    SQL_FIND_PAGE_BY_ENTITY("findPageByEntity", "<script>select <trim suffixOverrides=','>%s</trim> from %s <where>%s</where></script>"),
    /**
     * delete from %s where %s=%s
     */
    SQL_DELETE_BY_ID("delteById", "<script>delete from %s where %s=#{%s}</script>"),
    /**
     * update %s set %s where %s=%s
     */
    SQL_UPDATE_BY_ID("updateById", "<script>update %s set <trim suffixOverrides=','>%s</trim> where %s=#{%s}</script>"),
    /**
     * update %s set %s where %s
     */
    SQL_UPDATE_NOT_NULL_BY_ID("updateNotNullById", "<script>update %s set <trim suffixOverrides=','>%s</trim> where %s=#{%s}</script>"),
    /**
     * update %s set %s where %s
     */
    SQL_UPDATE_NOT_NULL_BY_ENTITY("updateNotNullByEntity", "<script>update %s set <trim suffixOverrides=','>%s</trim> <where>%s</where></script>");
    /**
     * if test='%s' %s if
     */
    public final static String IF = "<if test='%s!=null'>%s</if>";
    private String command;
    private String script;

    SqlCommand(String command, String script) {
        this.command = command;
        this.script = script;
    }

    public String getCommand() {
        return this.command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getScript() {
        return this.script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
