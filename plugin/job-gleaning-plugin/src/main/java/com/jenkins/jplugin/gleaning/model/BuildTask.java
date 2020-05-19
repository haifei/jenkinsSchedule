package com.jenkins.jplugin.gleaning.model;


import com.jenkins.jplugin.gleaning.util.GleaningCheckUtil;

import java.util.List;

/**
 * 2020-04-21  add by wanghf
 */
public class BuildTask implements Runnable{
    private List<BuildParameter> parameterList;

    public BuildTask(List<BuildParameter> parameterList) {
        this.parameterList = parameterList;
    }

    @Override
    public void run() {
        for (BuildParameter parameter : parameterList) {
            GleaningCheckUtil.doBuild(parameter.getProject(), parameter.getContext(), parameter.getType());
        }
    }
}
