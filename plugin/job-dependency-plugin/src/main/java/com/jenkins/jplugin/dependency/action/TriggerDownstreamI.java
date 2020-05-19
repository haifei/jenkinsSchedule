package com.jenkins.jplugin.dependency.action;

import hudson.model.Run;

import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public interface TriggerDownstreamI {

    //在job的构建历史中查找是否符合条件的Run
    boolean check();

    //在job'的构建历史中查找符合条件的Run,并记录下来
    List<Run> obtain();

}
