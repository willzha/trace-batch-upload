package com.baidu.trace.util;

/**
 * 时间工具类
 * 
 * @author baidu
 *
 */
public class TimeUtils {

    public static final int MILLIS_TO_SECONDS = 1000;

    public static final int MILLIS_TO_MINUTES = 60 * 1000;

    /**
     * 获取当前分钟数
     * 
     * @return
     */
    public static long getCurrentTimeOfMinutes() {
        return System.currentTimeMillis() / MILLIS_TO_MINUTES;
    }

}
