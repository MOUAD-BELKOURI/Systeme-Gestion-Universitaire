package com.universite.service;

import com.universite.dto.LoginRequest;
import com.universite.dto.RegisterRequest;

public interface AuthService {
    boolean authenticate(LoginRequest loginRequest);
    void register(RegisterRequest registerRequest);
}