package com.jenkins.jplugin.dependency.enums;

import hudson.model.Result;

/**
 * 2020-04-12 add by wanghf
 */
public enum ResultCondition {

    SUCCESS("Success") {
        @Override
        public boolean isMet(Result result) {
            return result == Result.SUCCESS;
        }
    },

    UNSTABLE("Unstable") {
        @Override
        public boolean isMet(Result result) {
            return result.isBetterOrEqualTo(Result.UNSTABLE);
        }
    },

    FAILED("Failed") {
        @Override
        public boolean isMet(Result result) {
            return result.isBetterOrEqualTo(Result.FAILURE);
        }
    };

    ResultCondition(String displayName) {
        this.displayName = displayName;
    }

    public final String displayName;

    public abstract boolean isMet(Result result);
}
