package com.hikvision.entity;

public class ShowIdentity {
    String token;
    String action;

    public ShowIdentity(String token, String action) {
        this.token = token;
        this.action = action;
    }

    public String getToken() {
        return token;
    }

    public String getAction() {
        return action;
    }
}

