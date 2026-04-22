package com.hcmute.edu.vn.focus_life.core.exception;

public class UserFacingException extends Exception {
    private final String userMessage;

    public UserFacingException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public UserFacingException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
