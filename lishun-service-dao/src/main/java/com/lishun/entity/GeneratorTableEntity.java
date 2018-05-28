package com.lishun.entity;

import java.util.List;

/**
 * 表数据
 *
 * @author lipengjun
 * @email 939961241@qq.com
 * @date 2016年12月20日 上午12:02:55
 */
public class GeneratorTableEntity {
    //表的名称
    private String tableName;
    //表的备注
    private String comments;
    //表的主键
    private GeneratorColumnEntity pk;
    //表的列名(不包含主键)
    private List<GeneratorColumnEntity> columns;

    //类名(第一个字母大写)，如：sys_user => SysUser
    private String className;
    //类名(第一个字母小写)，如：sys_user => sysUser
    private String classname;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public GeneratorColumnEntity getPk() {
        return pk;
    }

    public void setPk(GeneratorColumnEntity pk) {
        this.pk = pk;
    }

    public List<GeneratorColumnEntity> getColumns() {
        return columns;
    }

    public void setColumns(List<GeneratorColumnEntity> columns) {
        this.columns = columns;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }
}
