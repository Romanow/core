package ru.romanow.rest.client.model;

/**
 * Created by ronin on 13.02.16
 */
public class TestAuthResponse {
    private String uin;
    private Long expiredIn;
    private Boolean active;

    public TestAuthResponse() {}

    public TestAuthResponse(String uin, Long expiredIn, Boolean active) {
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
