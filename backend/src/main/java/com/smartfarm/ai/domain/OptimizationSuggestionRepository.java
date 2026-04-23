package com.smartfarm.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OptimizationSuggestionRepository extends JpaRepository<OptimizationSuggestion, Long> {
    List<OptimizationSuggestion> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, OptimizationSuggestion.SuggestionStatus status);
    List<OptimizationSuggestion> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
