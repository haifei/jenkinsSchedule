package com.jenkins.jplugin.dependency.exception;

/**
 * 2020-04-12 add by wanghf
 */
public class JobDependencyException extends Exception {

    public JobDependencyException() {
    }

    public JobDependencyException(String message) {
        super(message);
    }

    public JobDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
