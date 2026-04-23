package com.smartfarm.crop.infrastructure;

import com.smartfarm.crop.application.CropTemplateService;
import com.smartfarm.crop.application.GreenhousePlantingService;
import com.smartfarm.crop.domain.CropTemplate;
import com.smartfarm.crop.domain.GreenhousePlanting;
import com.smartfarm.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CropController {

    private final CropTemplateService templateService;
    private final GreenhousePlantingService plantingService;

    // ===== 种植模板 =====

    @GetMapping("/crop-templates")
    public List<CropTemplate> listTemplates(@RequestParam(required = false) Boolean systemOnly) {
        if (Boolean.TRUE.equals(systemOnly)) return templateService.listSystemTemplates();
        return templateService.listTemplates(SecurityUtils.currentTenantId());
    }

    @GetMapping("/crop-templates/{id}")
    public CropTemplate getTemplate(@PathVariable Long id) {
        return templateService.getById(id);
    }

    @PostMapping("/crop-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public CropTemplate createTemplate(@RequestBody CropTemplate template) {
        return templateService.create(template);
    }

    @PutMapping("/crop-templates/{id}")
    public CropTemplate updateTemplate(@PathVariable Long id, @RequestBody CropTemplate template) {
        return templateService.update(id, template);
    }

    @DeleteMapping("/crop-templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long id) {
        templateService.delete(id);
    }

    // ===== 大棚种植记录 =====

    @GetMapping("/greenhouse-plantings")
    public List<GreenhousePlanting> listPlantings(
            @RequestParam(required = false) String greenhouseNo) {
        if (greenhouseNo != null) return plantingService.listByGreenhouse(greenhouseNo);
        return plantingService.listByTenant(SecurityUtils.currentTenantId());
    }

    @PostMapping("/greenhouse-plantings")
    @ResponseStatus(HttpStatus.CREATED)
    public GreenhousePlanting createPlanting(@RequestBody CreatePlantingRequest req) {
        return plantingService.create(SecurityUtils.currentTenantId(), req.greenhouseNo(), req.templateId(), req.plantingDate());
    }

    @GetMapping("/greenhouse-plantings/{id}/stage")
    public Map<String, Object> getStageInfo(@PathVariable Long id) {
        return plantingService.getStageInfo(id);
    }

    record CreatePlantingRequest(String greenhouseNo, Long templateId, LocalDate plantingDate) {}
}
