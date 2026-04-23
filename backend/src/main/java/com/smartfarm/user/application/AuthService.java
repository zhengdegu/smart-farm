package com.smartfarm.user.application;

import com.smartfarm.shared.exception.BusinessException;
import com.smartfarm.shared.security.JwtProvider;
import com.smartfarm.user.domain.SysUser;
import com.smartfarm.user.domain.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public Map<String, Object> login(String username, String password) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "USER_DISABLED", "账号已禁用");
        }
        String token = jwtProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getRole().name());
        String refresh = jwtProvider.generateRefreshToken(user.getId());
        return Map.of(
                "token", token,
                "refresh_token", refresh,
                "expires_in", 7200,
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "nickname", user.getNickname() != null ? user.getNickname() : "",
                        "role", user.getRole().name()
                )
        );
    }

    @Transactional
    public SysUser register(Long tenantId, String username, String password,
                             String nickname, SysUser.UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(role);
        return userRepository.save(user);
    }

    public SysUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "用户不存在"));
    }
}
