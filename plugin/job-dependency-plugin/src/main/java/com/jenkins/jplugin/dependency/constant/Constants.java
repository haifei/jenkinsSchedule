package com.jenkins.jplugin.dependency.constant;

import java.util.regex.Pattern;

/**
 * 2020-04-12 add by wanghf
 */
public class Constants {

    public static final String TOKEN = "time_hour";
    public static final String DAY_OF_LAST_MONTH = "#$";
    public static final String DAY_OF_ANY_MONTH = "@$";
    public static final String DATE_OF_CALCULATION_CONTEXT = "*$";
    public static final String DATE_RELY_ON_ONESELF = "%$";
    public static final String JOB_PARAM_JOINER = ".";
    public static final String MULTI_JOB_PARAM_JOINER = ",";

    public static final String TOKEN_VERIFICATION = "^(19|20)\\d\\d/(0[1-9]|1[012])/(0[1-9]|[12][0-9]|3[01])/([01][0-9]|2[0-3])$";
    public static final String UPSTREAM_JOB_PARAMS_FORMAT = "^(\\s*.+\\s*,)*\\s*.+\\s*[^,]$";

    public static final Integer HOUR_START = 0;
    public static final Integer HOUR_END = 23;
    public static final Integer DAY_START = 1;
    public static final Integer DAY_END = 31;
    public static final Integer WEEK_START = 1;
    public static final Integer WEEK_END = 7;
    public static final Integer MONTH_START = 1;
    public static final Integer MONTH_END = 12;

    public static Pattern pattern = Pattern.compile(TOKEN_VERIFICATION);
    public static Pattern upstreamJobParamFormatPattern = Pattern.compile(UPSTREAM_JOB_PARAMS_FORMAT);

}
