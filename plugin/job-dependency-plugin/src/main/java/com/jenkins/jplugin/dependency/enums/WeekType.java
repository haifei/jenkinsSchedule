package com.jenkins.jplugin.dependency.enums;

import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;

/**
 * 2020-04-12 add by wanghf
 */
public enum WeekType {

    MONDAY("星期一", "1"),
    TUESDAY("星期二", "2"),
    WEDNESDAY("星期三", "3"),
    THURSDAY("星期四", "4"),
    FRIDAY("星期五", "5"),
    SATURDAY("星期六", "6"),
    SUNDAY("星期日", "7");

    public String week;
    public String week_num;

    WeekType(String week, String week_num) {
        this.week = week;
        this.week_num = week_num;
    }

    public static WeekType parse(String week) {
        for (WeekType type : WeekType.values()) {
            if (week.equals(type.week)) {
                return type;
            }
        }
        throw new JobDependencyRuntimeException(String.format("找不到对应的日期:%s", week));
    }
}
