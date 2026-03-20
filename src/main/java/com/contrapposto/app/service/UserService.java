package com.contrapposto.app.service;

import com.contrapposto.app.dto.RegisterRequest;
import com.contrapposto.app.model.User;

public interface UserService {

    User register(RegisterRequest request);

    boolean emailExists(String email);
}