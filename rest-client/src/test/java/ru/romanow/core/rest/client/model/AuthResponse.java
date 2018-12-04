package ru.romanow.core.rest.client.model;

public class AuthResponse {
    private String uin;
    private Long expiredIn;
    private Boolean active;

    public AuthResponse() {}

    public AuthResponse(String uin, Long expiredIn, Boolean active) {
        this.uin = uin;
        this.expiredIn = expiredIn;
        this.active = active;
    }

    public String getUin() {
        return uin;
    }

    public Long getExpiredIn() {
        return expiredIn;
    }

    public Boolean getActive() {
        return active;
    }
}
