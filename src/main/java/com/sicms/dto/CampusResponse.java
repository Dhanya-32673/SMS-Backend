package com.sicms.dto;

import com.sicms.entity.Campus;
import java.time.OffsetDateTime;

public class CampusResponse {

    private Long id;
    private String name;
    private String code;
    private Integer displayOrder;
    private boolean active = true;
    private String description;
    private long totalStudents;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public CampusResponse() {
    }

    public CampusResponse(Campus campus, long totalStudents) {
        if (campus != null) {
            this.id = campus.getId();
            this.name = campus.getName();
            this.code = campus.getCode();
            this.displayOrder = campus.getDisplayOrder();
            this.active = campus.isActive();
            this.description = campus.getDescription();
            this.createdAt = campus.getCreatedAt();
            this.updatedAt = campus.getUpdatedAt();
        }
        this.totalStudents = totalStudents;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getStudentCount() {
        return totalStudents;
    }

    public void setStudentCount(long studentCount) {
        this.totalStudents = studentCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
