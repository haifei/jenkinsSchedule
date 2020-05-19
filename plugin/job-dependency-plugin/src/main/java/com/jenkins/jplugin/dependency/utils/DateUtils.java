package com.jenkins.jplugin.dependency.utils;

import com.jenkins.jplugin.dependency.exception.JobDependencyException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 2020-04-12 add by wanghf
 */
public class DateUtils {


    //.TODO 思考下这里是否也存在线程安全的问题
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

    public static Date parseDate(String token) throws JobDependencyException {
        Date _tokenDate;
        try {
            synchronized (LOCK) {
                _tokenDate = hour_sdf.parse(token);
            }
        } catch (ParseException e) {
            throw new JobDependencyException(String.format("time_hour parse error.time_hour:%s.Support date format:yyyy/MM/dd/HH", token), e);
        } catch (NumberFormatException e) {
            throw new JobDependencyException(String.format("time_hour error.time_hour:%s", token), e);
        }
        return _tokenDate;
    }

}
