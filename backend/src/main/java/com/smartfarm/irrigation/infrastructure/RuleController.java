package com.smartfarm.irrigation.infrastructure;

import com.smartfarm.irrigation.application.RuleService;
import com.smartfarm.irrigation.domain.IrrigationRule;
import com.smartfarm.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public List<IrrigationRule> list() {
        return ruleService.listRules(SecurityUtils.currentTenantId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationRule create(@RequestBody IrrigationRule rule) {
        return ruleService.createRule(SecurityUtils.currentTenantId(), rule);
    }

    @PutMapping("/{ruleId}")
    public IrrigationRule update(@PathVariable Long ruleId, @RequestBody IrrigationRule rule) {
        return ruleService.updateRule(ruleId, rule);
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long ruleId) {
        ruleService.deleteRule(ruleId);
    }

    @PostMapping("/{ruleId}/toggle")
    public void toggle(@PathVariable Long ruleId) {
        ruleService.toggleRule(ruleId);
    }
}
