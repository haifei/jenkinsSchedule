package com.jenkins.jplugin.dependency.context;

import com.jenkins.jplugin.dependency.enums.DateType;
import com.jenkins.jplugin.dependency.enums.WeekType;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;
import com.jenkins.jplugin.dependency.utils.DateUtils;
import hudson.model.ParameterValue;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 2020-04-12 add by wanghf
 */
public class DateTypeValue {

    private ParameterValue token;

    private Date tokenDate;

    private SimpleDateFormat sdf;

    public DateTypeValue(ParameterValue token) throws JobDependencyException {
        this.token = token;
        tokenDate = DateUtils.parseDate(String.valueOf(this.token.getValue()));
    }

    public String dateType(DateType dateType) {
        String date = null;
        switch (dateType) {
            case HOUR:
                sdf = new SimpleDateFormat("HH");
                date = sdf.format(tokenDate);
                break;
            case DAY:
                sdf = new SimpleDateFormat("dd");
                date = sdf.format(tokenDate);
                break;
            case WEEK:
                sdf = new SimpleDateFormat("EE");
                date = WeekType.parse(sdf.format(tokenDate)).week_num;
                break;
            case MONTH:
                sdf = new SimpleDateFormat("MM");
                date = sdf.format(tokenDate);
                break;
            default:
                throw new JobDependencyRuntimeException(String.format("不支持的类型.DateType:%s", dateType));
        }
        return date;
    }
}
