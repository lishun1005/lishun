package com.lishun.utils;

import com.lishun.common.constant.Constant;
import com.lishun.common.utils.DateUtils;
import com.lishun.common.utils.RRException;
import com.lishun.entity.GeneratorColumnEntity;
import com.lishun.entity.GeneratorTableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
* @Description: 代码生成器   工具类
* @param
* @return
* @throws
* @author lishun
* @date 2018/5/28 0028 14:24
*/
public class GenUtils {

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/Dao.java.vm");
        templates.add("template/Dao.xml.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/ServiceImpl.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/list.html.vm");
        templates.add("template/list.js.vm");
        templates.add("template/menu.sql.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();

        //表信息
        GeneratorTableEntity GeneratorTableEntity = new GeneratorTableEntity();
        GeneratorTableEntity.setTableName(table.get("tableName"));
        GeneratorTableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(GeneratorTableEntity.getTableName(), config.getString("tablePrefix"));
        GeneratorTableEntity.setClassName(className);
        GeneratorTableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<GeneratorColumnEntity> columsList = new ArrayList<GeneratorColumnEntity>();
        for (Map<String, String> column : columns) {
            GeneratorColumnEntity GeneratorColumnEntity = new GeneratorColumnEntity();
            GeneratorColumnEntity.setColumnName(column.get("columnName"));
            GeneratorColumnEntity.setDataType(column.get("dataType"));
            GeneratorColumnEntity.setComments(column.get("columnComment"));
            GeneratorColumnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(GeneratorColumnEntity.getColumnName());
            GeneratorColumnEntity.setAttrName(attrName);
            GeneratorColumnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(GeneratorColumnEntity.getDataType(), "String");
            GeneratorColumnEntity.setAttrType(attrType);

            //是否主键
            if ("ORACLE".equals(Constant.USE_DATA)) {
                if ((column.get("columnName").equalsIgnoreCase(column.get("columnKey")) && GeneratorTableEntity.getPk() == null)) {
                    GeneratorTableEntity.setPk(GeneratorColumnEntity);
                }
            } else {
                if (("PRI".equalsIgnoreCase(column.get("columnKey")) && GeneratorTableEntity.getPk() == null)) {
                    GeneratorTableEntity.setPk(GeneratorColumnEntity);
                }
            }

            columsList.add(GeneratorColumnEntity);
        }
        GeneratorTableEntity.setColumns(columsList);

        //若没主键
        if (GeneratorTableEntity.getPk() == null) {
            //设置columnName为id的为主键
            boolean flag = true;
            for (GeneratorColumnEntity GeneratorColumnEntity : GeneratorTableEntity.getColumns()) {
                if ("id".equals(GeneratorColumnEntity.getAttrname())) {
                    GeneratorTableEntity.setPk(GeneratorColumnEntity);
                    flag = false;
                    break;
                }
            }
            //若无id字段则第一个字段为主键
            if (flag) {
                GeneratorTableEntity.setPk(GeneratorTableEntity.getColumns().get(0));
            }
        }


        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

        //封装模板数据
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tableName", GeneratorTableEntity.getTableName());
        map.put("comments", GeneratorTableEntity.getComments());
        map.put("pk", GeneratorTableEntity.getPk());
        map.put("className", GeneratorTableEntity.getClassName());
        map.put("classname", GeneratorTableEntity.getClassname());
        map.put("pathName", GeneratorTableEntity.getClassname().toLowerCase());
        map.put("columns", GeneratorTableEntity.getColumns());
        map.put("package", config.getString("package"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, GeneratorTableEntity.getClassName(), config.getString("package"))));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RRException("渲染模板失败，表名：" + GeneratorTableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator;
        }

        if (template.contains("Entity.java.vm")) {
            return packagePath + "entity" + File.separator + className + "Entity.java";
        }

        if (template.contains("Dao.java.vm")) {
            return packagePath + "dao" + File.separator + className + "Dao.java";
        }

        if (template.contains("Dao.xml.vm")) {
            return packagePath + "dao" + File.separator + className + "Dao.xml";
        }

        if (template.contains("Service.java.vm")) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains("ServiceImpl.java.vm")) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        if (template.contains("Controller.java.vm")) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }

        if (template.contains("list.html.vm")) {
            return "main" + File.separator + "webapp" + File.separator + "WEB-INF" + File.separator + "page"
                    + File.separator + "shop" + File.separator + className.toLowerCase() + ".html";
        }

        if (template.contains("list.js.vm")) {
            return "main" + File.separator + "webapp" + File.separator + "js" + File.separator + "shop" + File.separator + className.toLowerCase() + ".js";
        }

        if (template.contains("menu.sql.vm")) {
            return className.toLowerCase() + "_menu.sql";
        }

        return null;
    }
}
