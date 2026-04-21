package com.smartfarm.user.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sys_user")
@Getter @Setter
@NoArgsConstructor
public class SysUser extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.OPERATOR;

    @Column(length = 64)
    private String openid;

    @Column(length = 16)
    private String status = "ACTIVE";

    public enum UserRole { ADMIN, OPERATOR, VIEWER }
}
