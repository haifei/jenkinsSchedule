package com.jenkins.jplugin.dependency.exception;

/**
 * 2020-04-12 add by wanghf
 */
public class JobDependencyRuntimeException extends RuntimeException {

    public JobDependencyRuntimeException() {
    }

    public JobDependencyRuntimeException(String message) {
        super(message);
    }

    public JobDependencyRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobDependencyRuntimeException(Throwable cause) {
        super(cause);
    }

}
