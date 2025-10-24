package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String email;
    private String username;
    private String message;

    public AuthResponse(String token, String email, String username) {
        this.token = token;
        this.email = email;
        this.username = username;
    }
}