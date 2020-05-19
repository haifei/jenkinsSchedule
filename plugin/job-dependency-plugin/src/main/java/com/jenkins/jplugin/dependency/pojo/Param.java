package com.jenkins.jplugin.dependency.pojo;

/**
 * 2020-04-12 add by wanghf
 */
public class Param {

    String param;

    public Param(String param) {
        this.param = param;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    @Override
    public String toString(){
        return param;
    }
}



