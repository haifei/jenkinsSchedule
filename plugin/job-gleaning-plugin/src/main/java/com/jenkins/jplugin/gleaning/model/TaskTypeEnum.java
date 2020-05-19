package com.jenkins.jplugin.gleaning.model;

/**
 * 2020-04-21  add by wanghf
 */
public enum TaskTypeEnum {

    CRON_TASK("cron_task"),
    FAILURE_TASK("failure_task");

    private final String text;

    private TaskTypeEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
