package com.tictactoe.dto;

import java.time.Instant;

/**
 * Public DTO representing user profile details without exposing internal persistence implementation.
 */
public class UserProfileResponse {

    private Long id;
    private String username;
    private Instant createdAt;
    private Instant updatedAt;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String username, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
