package com.zsr.log.dao;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;

public class DataSourceUtil {
    private static DataSource dataSource;

    static {
        dataSource = new ComboPooledDataSource("LogMonitor");
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource("LogMonitor");
        }
        return dataSource;
    }
}
