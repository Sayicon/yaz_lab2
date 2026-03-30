package com.yazlab.auth.dto;

public class ValidateResponse {
    private boolean valid;
    private String username;

    public ValidateResponse(boolean valid, String username) {
        this.valid = valid;
        this.username = username;
    }

    public boolean isValid()        { return valid; }
    public String getUsername()     { return username; }
    public void setValid(boolean valid)         { this.valid = valid; }
    public void setUsername(String username)    { this.username = username; }
}
