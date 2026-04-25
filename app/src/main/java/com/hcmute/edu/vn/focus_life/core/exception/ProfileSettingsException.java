package com.hcmute.edu.vn.focus_life.core.exception;

public class ProfileSettingsException extends UserFacingException {
    public ProfileSettingsException(String userMessage) {
        super(userMessage);
    }

    public ProfileSettingsException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
