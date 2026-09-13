package com.sicms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCampusRequest {

    @NotBlank(message = "Campus name is required")
    @Size(max = 100, message = "Campus name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Campus code cannot exceed 50 characters")
    private String code;

    private Integer displayOrder;

    private boolean active = true;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    public CreateCampusRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
