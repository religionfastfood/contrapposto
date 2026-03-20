package com.contrapposto.app.dto;

import com.contrapposto.app.model.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private Role role;
}