package com.smartfarm.crop.application;

import com.smartfarm.crop.domain.*;
import com.smartfarm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CropTemplateService {

    private final CropTemplateRepository templateRepo;

    public List<CropTemplate> listTemplates(Long tenantId) {
        return templateRepo.findByTenantIdOrIsSystemTrue(tenantId);
    }

    public List<CropTemplate> listSystemTemplates() {
        return templateRepo.findByIsSystemTrue();
    }

    public CropTemplate getById(Long id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new BusinessException("种植模板不存在: " + id));
    }

    @Transactional
    public CropTemplate create(CropTemplate template) {
        template.setIsSystem(false);
        return templateRepo.save(template);
    }

    @Transactional
    public CropTemplate update(Long id, CropTemplate updated) {
        CropTemplate existing = getById(id);
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException("系统预设模板不可修改");
        }
        existing.setName(updated.getName());
        existing.setCropType(updated.getCropType());
        existing.setStages(updated.getStages());
        return templateRepo.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        CropTemplate existing = getById(id);
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException("系统预设模板不可删除");
        }
        templateRepo.delete(existing);
    }
}
