package com.jenkins.jplugin.depgraph.job_depgraph.model.context;

/**
 * 2020-04-12 add by wanghf
 */
public enum JOB_STATE {
    BUILDING(1),
    SUCCESS(2),
    FAILURE(3),
    PEDDING(4),
    ABORTED(5),
    NOT_BUILT(6),
    UNKNOWN(7);

    private int value;

    private JOB_STATE(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
