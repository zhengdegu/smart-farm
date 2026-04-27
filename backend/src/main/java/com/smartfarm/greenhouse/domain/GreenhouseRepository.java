package com.smartfarm.greenhouse.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GreenhouseRepository extends JpaRepository<Greenhouse, Long> {

    List<Greenhouse> findByTenantId(Long tenantId);

    List<Greenhouse> findByTenantIdAndStatus(Long tenantId, Greenhouse.GreenhouseStatus status);

    boolean existsByTenantIdAndGreenhouseNo(Long tenantId, String greenhouseNo);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, Greenhouse.GreenhouseStatus status);
}
