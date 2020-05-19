package com.jenkins.jplugin.dependency.enums;

import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;

/**
 * 2020-04-12 add by wanghf
 */
public enum DateType {

    HOUR('H', "hour"), DAY('D', "day"), WEEK('W', "week"), MONTH('M', "month");

    public char value;
    public String type;

    DateType(char value, String type) {
        this.value = value;
        this.type = type;
    }

    public static DateType getDateType(char value) {
        for (DateType dateType : DateType.values()) {
            if (value == (dateType.value)) {
                return dateType;
            }
        }
        throw new JobDependencyRuntimeException(String.format("Not support datetype:%s", value));
    }

    public static DateType getDateType(String type) {
        for (DateType dateType : DateType.values()) {
            if (type.equals(dateType.value)) {
                return dateType;
            }
        }
        throw new JobDependencyRuntimeException(String.format("Not support datetype:%s", type));
    }

}
