package com.jenkins.jplugin.dependency.utils;

import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.enums.DateType;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.jenkins.jplugin.dependency.constant.Constants.*;

/**
 * 2020-04-12 add by wanghf
 */
public class DateRange {

    private static final Logger logger = LoggerFactory.getLogger(DateRange.class);

    public static void checkHourDate(String date) throws JobDependencyException {
        check(DateType.HOUR, date, HOUR_START, HOUR_END);
    }

    public static void checkHourdates(List<String> dates) throws JobDependencyException {
        for (String date : dates)
            checkHourDate(date);
    }

    public static void checkDayDate(String date) throws JobDependencyException {
        check(DateType.DAY, date, DAY_START, DAY_END);
    }

    public static void checkDayDates(List<String> dates) throws JobDependencyException {
        for (String date : dates) {
            if (!Constants.DAY_OF_LAST_MONTH.equals(date)
                    && !Constants.DAY_OF_ANY_MONTH.equals(date)) {
                checkDayDate(date);
            }
        }
    }

    public static void checkWeekDate(String date) throws JobDependencyException {
        check(DateType.WEEK, date, WEEK_START, WEEK_END);
    }

    public static void checkWeekDates(List<String> dates) throws JobDependencyException {
        for (String date : dates)
            checkWeekDate(date);
    }

    public static void checkMonthDate(String date) throws JobDependencyException {
        check(DateType.MONTH, date, MONTH_START, MONTH_END);
    }

    public static void checkMonthDates(List<String> dates) throws JobDependencyException {
        for (String date : dates)
            checkMonthDate(date);
    }

    private static void check(DateType dateType, String date, int start, int end) throws JobDependencyException {

        //这里特殊处理一下吧,方式不是很好
        if (date.contains(Constants.DAY_OF_LAST_MONTH)) {
            throw new JobDependencyException("条件参数格式错误.详见文档.");
        }

        try {
            int hour = Integer.parseInt(date);
            if (hour < start || hour > end) {
                throw new JobDependencyException(String.format("Date范围越界.Date type:%s.日期范围为:%s->%s,date:%s不在日期范围内.",
                        dateType.value, start, end, date));
            }
        } catch (NumberFormatException e) {
            logger.error(String.format("Date parse error.date:%s", date), e);
            throw new JobDependencyException(String.format("Date解析错误.date:%s", date));
        }
    }
}