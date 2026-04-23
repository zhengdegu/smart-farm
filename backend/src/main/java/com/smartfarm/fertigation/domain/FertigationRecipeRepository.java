package com.smartfarm.fertigation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FertigationRecipeRepository extends JpaRepository<FertigationRecipe, Long> {
    List<FertigationRecipe> findByTenantIdOrIsSystemTrue(Long tenantId);
    List<FertigationRecipe> findByCropTypeAndGrowthStage(String cropType, String growthStage);
}
