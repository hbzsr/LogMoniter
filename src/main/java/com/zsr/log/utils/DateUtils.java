package com.zsr.log.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期转换类
 */
public class DateUtils {
    private static SimpleDateFormat sdf;

    public static String getDateTime() {
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
}
