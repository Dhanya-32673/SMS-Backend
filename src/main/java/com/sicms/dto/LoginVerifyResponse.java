package com.sicms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginVerifyResponse {

    private boolean success;
    private boolean authenticated;
    private String token;
    private String accessToken;
    private Long expiresIn;
    private String refreshToken;
    private String redirectUrl;
    private String message;
    private UserDto user;

    public LoginVerifyResponse() {
    }

    public LoginVerifyResponse(boolean success, boolean authenticated, String token, Long expiresIn, String refreshToken, String redirectUrl, String message, UserDto user) {
        this.success = success;
        this.authenticated = authenticated;
        this.token = token;
        this.accessToken = token;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.redirectUrl = redirectUrl;
        this.message = message;
        this.user = user;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        this.accessToken = token;
    }

    public String getAccessToken() {
        return accessToken != null ? accessToken : token;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.token = accessToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}
