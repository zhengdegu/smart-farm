package com.smartfarm.irrigation.infrastructure;

import com.smartfarm.irrigation.application.RuleService;
import com.smartfarm.irrigation.domain.IrrigationRule;
import com.smartfarm.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Tag(name = "灌溉规则", description = "自动灌溉规则管理")
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    @Operation(summary = "规则列表")
    public List<IrrigationRule> list() {
        return ruleService.listRules(SecurityUtils.currentTenantId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建灌溉规则")
    public IrrigationRule create(@RequestBody IrrigationRule rule) {
        return ruleService.createRule(SecurityUtils.currentTenantId(), rule);
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "更新灌溉规则")
    public IrrigationRule update(@PathVariable Long ruleId, @RequestBody IrrigationRule rule) {
        return ruleService.updateRule(ruleId, rule);
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除灌溉规则")
    public void delete(@PathVariable Long ruleId) {
        ruleService.deleteRule(ruleId);
    }

    @PostMapping("/{ruleId}/toggle")
    @Operation(summary = "启用/禁用规则")
    public void toggle(@PathVariable Long ruleId) {
        ruleService.toggleRule(ruleId);
    }
}
