package com.sicms.dto;

import jakarta.validation.constraints.NotBlank;

public class DocumentRejectionRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;

    public DocumentRejectionRequest() {
    }

    public DocumentRejectionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
