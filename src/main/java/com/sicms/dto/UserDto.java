package com.sicms.dto;

import com.sicms.entity.User;

public class UserDto {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String authProvider;
    private String profilePhotoUrl;
    private Boolean mustChangePassword;
    private String studentId;

    public UserDto() {
    }

    public UserDto(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole() != null ? user.getRole().getRoleName().replace("ROLE_", "") : null;
        this.authProvider = user.getAuthProvider() != null ? user.getAuthProvider().name() : null;
        this.profilePhotoUrl = user.getProfilePhotoUrl();
        this.mustChangePassword = user.getMustChangePassword();
        if (user.getStudent() != null) {
            try {
                this.studentId = user.getStudent().getStudentId();
            } catch (Exception ignored) {
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
