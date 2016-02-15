package ru.romanow.rest.client.model;

/**
 * Created by ronin on 13.02.16
 */
public class AuthRequest {
    private String login;
    private String password;

    public AuthRequest() {}

    public AuthRequest(String login, String password) {
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
