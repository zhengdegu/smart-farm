package com.smartfarm.crop.application;

import com.smartfarm.crop.domain.*;
import com.smartfarm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GreenhousePlantingService {

    private final GreenhousePlantingRepository plantingRepo;
    private final CropTemplateRepository templateRepo;

    public List<GreenhousePlanting> listByTenant(Long tenantId) {
        return plantingRepo.findByTenantId(tenantId);
    }

    public List<GreenhousePlanting> listActive(Long tenantId) {
        return plantingRepo.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    public List<GreenhousePlanting> listByGreenhouse(String greenhouseNo) {
        return plantingRepo.findByGreenhouseNo(greenhouseNo);
    }

    public GreenhousePlanting getById(Long id) {
        return plantingRepo.findById(id)
                .orElseThrow(() -> new BusinessException("种植记录不存在: " + id));
    }

    @Transactional
    public GreenhousePlanting create(Long tenantId, String greenhouseNo, Long templateId, LocalDate plantingDate) {
        CropTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() -> new BusinessException("种植模板不存在: " + templateId));

        GreenhousePlanting planting = new GreenhousePlanting();
        planting.setTenantId(tenantId);
        planting.setGreenhouseNo(greenhouseNo);
        planting.setCropTemplate(template);
        planting.setPlantingDate(plantingDate);

        // 计算预计收获日期（所有阶段天数之和）
        int totalDays = calcTotalDays(template);
        planting.setExpectedHarvestDate(plantingDate.plusDays(totalDays));

        // 设置当前阶段
        planting.setCurrentStage(getCurrentStageName(template, plantingDate));

        return plantingRepo.save(planting);
    }

    /**
     * 获取当前生长阶段及灌溉建议
     */
    public Map<String, Object> getStageInfo(Long plantingId) {
        GreenhousePlanting planting = getById(plantingId);
        CropTemplate template = planting.getCropTemplate();
        LocalDate today = LocalDate.now();
        long daysSincePlanting = ChronoUnit.DAYS.between(planting.getPlantingDate(), today);

        Map<String, Object> result = new HashMap<>();
        result.put("plantingId", plantingId);
        result.put("greenhouseNo", planting.getGreenhouseNo());
        result.put("cropType", template.getCropType());
        result.put("templateName", template.getName());
        result.put("plantingDate", planting.getPlantingDate());
        result.put("daysSincePlanting", daysSincePlanting);

        // 找到当前阶段
        int accDays = 0;
        Map<String, Object> currentStage = null;
        for (Map<String, Object> stage : template.getStages()) {
            int duration = ((Number) stage.get("duration_days")).intValue();
            if (daysSincePlanting >= accDays && daysSincePlanting < accDays + duration) {
                currentStage = stage;
                result.put("daysInStage", daysSincePlanting - accDays);
                result.put("stageDaysRemaining", accDays + duration - daysSincePlanting);
                break;
            }
            accDays += duration;
        }

        if (currentStage != null) {
            result.put("currentStage", currentStage);
            result.put("recommendation", "正常灌溉");
        } else {
            result.put("currentStage", null);
            result.put("recommendation", daysSincePlanting >= accDays ? "已超出生长周期，建议收获" : "尚未开始");
        }

        // 更新当前阶段
        String stageName = currentStage != null ? (String) currentStage.get("name") : null;
        if (stageName != null && !stageName.equals(planting.getCurrentStage())) {
            planting.setCurrentStage(stageName);
            plantingRepo.save(planting);
        }

        return result;
    }

    private int calcTotalDays(CropTemplate template) {
        return template.getStages().stream()
                .mapToInt(s -> ((Number) s.get("duration_days")).intValue())
                .sum();
    }

    private String getCurrentStageName(CropTemplate template, LocalDate plantingDate) {
        long days = ChronoUnit.DAYS.between(plantingDate, LocalDate.now());
        int acc = 0;
        for (Map<String, Object> stage : template.getStages()) {
            int duration = ((Number) stage.get("duration_days")).intValue();
            if (days >= acc && days < acc + duration) {
                return (String) stage.get("name");
            }
            acc += duration;
        }
        return template.getStages().isEmpty() ? null : (String) template.getStages().get(0).get("name");
    }
}
