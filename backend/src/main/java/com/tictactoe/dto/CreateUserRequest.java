package com.tictactoe.dto;

/**
 * DTO for user profile registration.
 */
public class CreateUserRequest {

    private String username;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
