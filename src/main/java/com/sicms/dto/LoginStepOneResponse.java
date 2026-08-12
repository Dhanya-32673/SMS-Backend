package com.sicms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginStepOneResponse {

    private boolean success;
    private boolean otpRequired;
    private String message;

    public LoginStepOneResponse() {
    }

    public LoginStepOneResponse(boolean success, boolean otpRequired, String message) {
        this.success = success;
        this.otpRequired = otpRequired;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isOtpRequired() {
        return otpRequired;
    }

    public void setOtpRequired(boolean otpRequired) {
        this.otpRequired = otpRequired;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
