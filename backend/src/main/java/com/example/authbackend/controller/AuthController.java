package com.example.authbackend.controller;

import com.example.authbackend.dto.Result;
import com.example.authbackend.entity.User;
import com.example.authbackend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        return authService.register(user.getUsername(), user.getPassword(), user.getRole());
    }
    
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) {
        return authService.login(user.getUsername(), user.getPassword());
    }
    
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        return authService.logout(token);
    }
}
