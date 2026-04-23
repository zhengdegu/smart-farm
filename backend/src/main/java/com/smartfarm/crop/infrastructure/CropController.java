package com.smartfarm.crop.infrastructure;

import com.smartfarm.crop.application.CropTemplateService;
import com.smartfarm.crop.application.GreenhousePlantingService;
import com.smartfarm.crop.domain.CropTemplate;
import com.smartfarm.crop.domain.GreenhousePlanting;
import com.smartfarm.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "作物管理", description = "种植模板、大棚种植记录、生长阶段")
public class CropController {

    private final CropTemplateService templateService;
    private final GreenhousePlantingService plantingService;

    // ===== 种植模板 =====

    @GetMapping("/crop-templates")
    @Operation(summary = "种植模板列表")
    public List<CropTemplate> listTemplates(@RequestParam(required = false) Boolean systemOnly) {
        if (Boolean.TRUE.equals(systemOnly)) return templateService.listSystemTemplates();
        return templateService.listTemplates(SecurityUtils.currentTenantId());
    }

    @GetMapping("/crop-templates/{id}")
    @Operation(summary = "模板详情")
    public CropTemplate getTemplate(@PathVariable Long id) {
        return templateService.getById(id);
    }

    @PostMapping("/crop-templates")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建种植模板")
    public CropTemplate createTemplate(@RequestBody CropTemplate template) {
        return templateService.create(template);
    }

    @PutMapping("/crop-templates/{id}")
    @Operation(summary = "更新种植模板")
    public CropTemplate updateTemplate(@PathVariable Long id, @RequestBody CropTemplate template) {
        return templateService.update(id, template);
    }

    @DeleteMapping("/crop-templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除种植模板")
    public void deleteTemplate(@PathVariable Long id) {
        templateService.delete(id);
    }

    // ===== 大棚种植记录 =====

    @GetMapping("/greenhouse-plantings")
    @Operation(summary = "大棚种植记录列表")
    public List<GreenhousePlanting> listPlantings(
            @RequestParam(required = false) String greenhouseNo) {
        if (greenhouseNo != null) return plantingService.listByGreenhouse(greenhouseNo);
        return plantingService.listByTenant(SecurityUtils.currentTenantId());
    }

    @PostMapping("/greenhouse-plantings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建种植记录")
    public GreenhousePlanting createPlanting(@RequestBody CreatePlantingRequest req) {
        return plantingService.create(SecurityUtils.currentTenantId(), req.greenhouseNo(), req.templateId(), req.plantingDate());
    }

    @GetMapping("/greenhouse-plantings/{id}/stage")
    @Operation(summary = "查询生长阶段信息")
    public Map<String, Object> getStageInfo(@PathVariable Long id) {
        return plantingService.getStageInfo(id);
    }

    record CreatePlantingRequest(String greenhouseNo, Long templateId, LocalDate plantingDate) {}
}
