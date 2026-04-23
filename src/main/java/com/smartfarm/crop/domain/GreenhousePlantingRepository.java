package com.smartfarm.crop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GreenhousePlantingRepository extends JpaRepository<GreenhousePlanting, Long> {
    List<GreenhousePlanting> findByTenantId(Long tenantId);
    List<GreenhousePlanting> findByGreenhouseNo(String greenhouseNo);
    List<GreenhousePlanting> findByTenantIdAndStatus(Long tenantId, String status);
}
