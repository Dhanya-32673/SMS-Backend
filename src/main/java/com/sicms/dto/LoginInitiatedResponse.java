package com.sicms.dto;

public class LoginInitiatedResponse {
    private String message;
    private String email;
    private boolean requiresOtp;

    public LoginInitiatedResponse() {
    }

    public LoginInitiatedResponse(String message, String email, boolean requiresOtp) {
        this.message = message;
        this.email = email;
        this.requiresOtp = requiresOtp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }
}
