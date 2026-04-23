package com.smartfarm.crop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CropTemplateRepository extends JpaRepository<CropTemplate, Long> {
    List<CropTemplate> findByTenantIdOrIsSystemTrue(Long tenantId);
    List<CropTemplate> findByCropType(String cropType);
    List<CropTemplate> findByIsSystemTrue();
}
