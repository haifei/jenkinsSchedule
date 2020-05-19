package com.jenkins.jplugin.dependency.pojo;

import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.utils.Utils;

/**
 * 2020-04-12 add by wanghf
 */
public class JobParam extends Param {

    String name;

    public JobParam(String name, String param) {
        super(param);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return Utils.getStrings(new String[]{name, Constants.JOB_PARAM_JOINER, param});
    }
}
