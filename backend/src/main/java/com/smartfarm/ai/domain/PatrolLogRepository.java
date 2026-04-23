package com.smartfarm.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatrolLogRepository extends JpaRepository<PatrolLog, Long> {
    List<PatrolLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
