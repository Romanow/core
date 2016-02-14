package ru.romanow.rest.client.model;

/**
 * Created by ronin on 13.02.16
 */
public class TestAuthRequest {
    private String login;
    private String password;

    public TestAuthRequest() {}

    public TestAuthRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
