package com.jenkins.jplugin.dependency.exception;

/**
 * 2020-04-12 add by wanghf
 */
public class FormValidationException extends Exception {

    public FormValidationException() {
    }

    public FormValidationException(String message) {
        super(message);
    }

    public FormValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
