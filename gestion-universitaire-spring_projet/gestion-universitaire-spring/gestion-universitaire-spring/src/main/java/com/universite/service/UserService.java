package com.universite.service;

import com.universite.dto.RegisterRequest;
import com.universite.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User createUser(RegisterRequest registerRequest);
    User getUserById(Long id);
    User getUserByEmail(String email);
    boolean existsByEmail(String email);
    void deleteUser(Long id);
}