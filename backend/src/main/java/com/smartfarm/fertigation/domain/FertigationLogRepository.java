package com.smartfarm.fertigation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FertigationLogRepository extends JpaRepository<FertigationLog, Long> {
    List<FertigationLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<FertigationLog> findByGreenhouseNoOrderByCreatedAtDesc(String greenhouseNo);
}
