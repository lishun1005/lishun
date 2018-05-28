package com.lishun.service.impl;

import com.lishun.common.constant.Constant;
import com.lishun.common.utils.StringUtils;
import com.lishun.dao.GeneratorDao;
import com.lishun.service.GeneratorService;
import com.lishun.dao.OracleGeneratorDao;
import com.lishun.entity.GeneratorResultMap;
import com.lishun.utils.GenUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@Service("sysGeneratorService")
public class GeneratorServiceImpl implements GeneratorService {
    @Autowired
    private GeneratorDao generatorDao;

    @Autowired
    private OracleGeneratorDao oracleGeneratorDao;

    @Override
    public List<Map<String, Object>> queryList(Map<String, Object> map) {
        if ("ORACLE".equals(Constant.USE_DATA)) {
            List<Map<String, Object>> list = oracleGeneratorDao.queryList(map);

            //oracle需转为驼峰命名
            List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> stringObjectMap : list) {
                Map<String, Object> objectMap = new HashMap<String, Object>();
                for (String key : stringObjectMap.keySet()) {
                    String mapKey = StringUtils.lineToHump(key);
                    objectMap.put(mapKey, stringObjectMap.get(key));
                }
                mapList.add(objectMap);
            }
            return mapList;
        }
        return generatorDao.queryList(map);
    }

    @Override
    public int queryTotal(Map<String, Object> map) {
        if ("ORACLE".equals(Constant.USE_DATA)) {
            return oracleGeneratorDao.queryTotal(map);
        }
        return generatorDao.queryTotal(map);
    }

    @Override
    public Map<String, String> queryTable(String tableName) {
        if ("ORACLE".equals(Constant.USE_DATA)) {
            Map<String, String> objectMap = oracleGeneratorDao.queryTable(tableName);

            //oracle需转为驼峰命名
            Map<String, String> map = new HashMap<String, String>();
            for (String key : objectMap.keySet()) {
                String mapKey = StringUtils.lineToHump(key);
                map.put(mapKey, objectMap.get(key));
            }
            return map;
        }
        return generatorDao.queryTable(tableName);
    }

    @Override
    public List<Map<String, String>> queryColumns(String tableName) {
        if ("ORACLE".equals(Constant.USE_DATA)) {
            List<GeneratorResultMap> list = oracleGeneratorDao.queryColumns(tableName);

            //oracle
            List<Map<String, String>> mapList = new ArrayList<Map<String, String>>();
            for (GeneratorResultMap stringObjectMap : list) {
                // 获取实体类的所有属性，返回Field数组
                Field[] field = stringObjectMap.getClass().getDeclaredFields();

                Map<String, String> objectMap = new HashMap<String, String>();
                for (int j = 0; j < field.length; j++) {
                    // 获取属性的名字
                    String name = field[j].getName();
                    // 将属性的首字符大写，方便构造get，set方法
                    String Name = name.substring(0, 1).toUpperCase() + name.substring(1);

                    try {
                        Method m = stringObjectMap.getClass().getMethod("get" + Name);
                        // 调用getter方法获取属性值
                        String value = (String) m.invoke(stringObjectMap);
                        objectMap.put(name, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                mapList.add(objectMap);
            }
            return mapList;
        }
        return generatorDao.queryColumns(tableName);
    }

    @Override
    public byte[] generatorCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        for (String tableName : tableNames) {
            //查询表信息
            Map<String, String> table = queryTable(tableName);
            //查询列信息
            List<Map<String, String>> columns = queryColumns(tableName);
            //生成代码
            GenUtils.generatorCode(table, columns, zip);
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

}
