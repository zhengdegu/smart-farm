package com.smartfarm.user.infrastructure;

import com.smartfarm.user.application.AuthService;
import com.smartfarm.user.domain.SysUser;
import com.smartfarm.user.domain.SysUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "登录、用户列表")
public class AuthController {

    private final AuthService authService;
    private final SysUserRepository userRepository;

    @GetMapping("/users")
    @Operation(summary = "用户列表")
    public java.util.List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream().map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "username", u.getUsername(),
                "nickname", u.getNickname() != null ? u.getNickname() : "",
                "role", u.getRole().name(),
                "status", u.getStatus() != null ? u.getStatus() : "ACTIVE"
        )).toList();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "返回 JWT token")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.getUsername(), req.getPassword());
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }
}
