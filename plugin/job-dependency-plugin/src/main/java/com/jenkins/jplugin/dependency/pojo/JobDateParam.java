package com.jenkins.jplugin.dependency.pojo;

import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.utils.Utils;

/**
 * 2020-04-12 add by wanghf
 */
public class JobDateParam extends JobParam {

    private String date;

    public JobDateParam(String name, String date, String param) {
        super(name, param);
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return Utils.getStrings(new String[]{name, Constants.JOB_PARAM_JOINER, date, Constants.JOB_PARAM_JOINER, param});
    }
}
