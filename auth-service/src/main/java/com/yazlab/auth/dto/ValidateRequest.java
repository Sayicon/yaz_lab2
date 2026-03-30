package com.yazlab.auth.dto;

public class ValidateRequest {
    private String token;

    public ValidateRequest() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
