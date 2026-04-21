package com.smartfarm.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findByOpenid(String openid);

    boolean existsByUsername(String username);
}
