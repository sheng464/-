package com.example.authbackend.service;

import com.example.authbackend.dto.LoginResponse;
import com.example.authbackend.dto.Result;
import com.example.authbackend.entity.User;
import com.example.authbackend.util.JwtUtil;
import com.example.authbackend.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RedisUtil redisUtil;
    
    public Result<?> register(String username, String password, String role) {
        if (userService.existsByUsername(username)) {
            return Result.error("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role != null && !role.isEmpty() ? role : "USER");
        
        userService.save(user);
        
        return Result.success("注册成功");
    }
    
    public Result<LoginResponse> login(String username, String password) {
        User user = userService.findByUsername(username);
        
        if (user == null || !user.getPassword().equals(password)) {
            return Result.error("用户名或密码错误");
        }
        
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        redisUtil.setToken(user.getUsername(), token);
        
        LoginResponse loginResponse = new LoginResponse(token, user.getUsername(), user.getRole());
        
        return Result.success(loginResponse);
    }
    
    public Result<?> logout(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error("无效的令牌格式");
        }
        
        token = token.substring(7);
        
        try {
            String username = jwtUtil.getUsernameFromToken(token);
            redisUtil.removeToken(username);
            return Result.success("退出成功");
        } catch (Exception e) {
            return Result.error("无效的令牌");
        }
    }
}
