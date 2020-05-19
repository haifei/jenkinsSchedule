package com.jenkins.jplugin.batchBuild.utils;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public class DateUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);
    private static Calendar calendar = Calendar.getInstance();
    private static final Integer DEFAULT_HOUR_OR_MONTH_TIME = 00;
    private static final Integer DEFAULT_DAY_TIME = 01;

    private static final SimpleDateFormat hour_sdf = new SimpleDateFormat("yyyy/MM/dd/HH");
    private static final SimpleDateFormat day_sdf = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat week_sdf = day_sdf; //获取星期的第一天
    private static final SimpleDateFormat month_sdf = new SimpleDateFormat("yyyy/MM");

    private static final Object LOCK = new Object();


    /**
     * 取得当前日期所在周的第一天
     *
     * @param date
     * @return
     */
    public static Date getFirstDayOfWeek(Date date) {
        calendar.clear();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_WEEK,
                calendar.getFirstDayOfWeek()); // MONTH
        return calendar.getTime();
    }

    /**
     * 对指定日期进行在一周内day的增加
     *
     * @param date
     * @param week
     * @return
     */
    public static Date getSpecifyDayOfWeek(Date date, int week) {
        getFirstDayOfWeek(date);
        calendar.add(Calendar.DAY_OF_WEEK, week - DEFAULT_DAY_TIME);
        return calendar.getTime();
    }

    /**
     * 获取当前日期所在月的第一天,从1开始
     *
     * @param date
     * @return
     */
    public static Date getFirstDayOfMonth(Date date) {
        calendar.clear();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, DEFAULT_DAY_TIME);
        return calendar.getTime();
    }

    /**
     * 对指定日期一月内day的增加
     *
     * @param date
     * @param day
     * @return
     */
    public static Date getSpecifyDayOfMonth(Date date, int day) {
        getFirstDayOfMonth(date);
        calendar.add(Calendar.DAY_OF_MONTH, day - 1);
        return calendar.getTime();
    }

    /**
     * 对指定的日期进行增减操作
     *
     * @param date
     * @param day
     * @return
     */
    public static Date getTokenDayOfMonth(Date date, int day) {
        calendar.clear();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    }

    /**
     * 获取月的最后一天
     *
     * @param date
     * @return
     */
    public static Date getLastDayOfMonth(Date date) {
        calendar.clear();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    /**
     * 获取当前日期所在天的第一个小时,从0开始
     *
     * @param date
     * @return
     */
    public static Date getTokenHourOfDay(Date date, int hour) {
        calendar.clear();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hour);
        return calendar.getTime();
    }

    /**
     * 对当前小时进行增减操作
     *
     * @param date
     * @return
     */
    public static Date getFirstHourOfDay(Date date) {
        calendar.clear();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_HOUR_OR_MONTH_TIME);
        return calendar.getTime();
    }

    /**
     * 对当前的日期进行小时的增加
     *
     * @param date
     * @param hour
     * @return
     */
    public static Date getSpecifyHourOfDay(Date date, int hour) {
        getFirstHourOfDay(date);
        calendar.add(Calendar.HOUR_OF_DAY, hour);
        return calendar.getTime();
    }

    /**
     * 获取当前日期所在年的第一个月,数字从0开始,但是月份依然从1开始
     *
     * @param date
     * @return
     */
    public static Date getFirstMonthOfYear(Date date) {
        calendar.clear();
        calendar.setTime(date);
        calendar.set(Calendar.MONTH, DEFAULT_HOUR_OR_MONTH_TIME);
        return calendar.getTime();
    }

    /**
     * 对当前月进行增减操作
     *
     * @param date
     * @return
     */
    public static Date getTokenMonthOfYear(Date date, int month) {
        calendar.clear();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, month);
        return calendar.getTime();
    }

    /**
     * 对指定日期年对月的增加
     *
     * @param date
     * @param month
     * @return
     */
    public static Date getSpecifyMonthOfYear(Date date, int month) {
        getFirstMonthOfYear(date);
        calendar.add(Calendar.MONTH, month - 1);
        return calendar.getTime();
    }

    public static String getStringOfDay(Date date) {
        return day_sdf.format(date);
    }

    public static String getStringOfWeek(Date date) {
        return week_sdf.format(getFirstDayOfWeek(date));
    }

    public static String getStringOfMonth(Date date) {
        return month_sdf.format(date);
    }


    /**
     * 获取 日期范围
     */
    public static List<String> getDateRangebyDay(String startTime, String endTime) {
        if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
            return null;
        }
        List<String> dateList = Lists.newArrayList();


        try {
            calendar.clear();
            calendar.setTime(hour_sdf.parse(startTime));
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(hour_sdf.parse(endTime));

            dateList.add(startTime);
            while (calendar.before(endCalendar)){
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                dateList.add(hour_sdf.format(calendar.getTime()));
            }
        } catch (Exception e) {
            logger.error("getDateRangebyDay 解析错误:" + e);
        }
        return dateList;
    }
}
