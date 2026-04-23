package com.smartfarm.irrigation.application;

import com.smartfarm.irrigation.domain.IrrigationRule;
import com.smartfarm.irrigation.domain.IrrigationRuleRepository;
import com.smartfarm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final IrrigationRuleRepository ruleRepository;

    public List<IrrigationRule> listRules(Long tenantId) {
        return ruleRepository.findByTenantId(tenantId);
    }

    public List<IrrigationRule> getEnabledRules() {
        return ruleRepository.findByEnabledTrue();
    }

    @Transactional
    public IrrigationRule createRule(Long tenantId, IrrigationRule rule) {
        rule.setTenantId(tenantId);
        return ruleRepository.save(rule);
    }

    @Transactional
    public IrrigationRule updateRule(Long ruleId, IrrigationRule update) {
        IrrigationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RULE_NOT_FOUND", "规则不存在"));
        if (update.getName() != null) rule.setName(update.getName());
        if (update.getRuleType() != null) rule.setRuleType(update.getRuleType());
        if (update.getDeviceId() != null) rule.setDeviceId(update.getDeviceId());
        if (update.getSensorDeviceId() != null) rule.setSensorDeviceId(update.getSensorDeviceId());
        if (update.getThresholdLow() != null) rule.setThresholdLow(update.getThresholdLow());
        if (update.getThresholdHigh() != null) rule.setThresholdHigh(update.getThresholdHigh());
        if (update.getCronExpression() != null) rule.setCronExpression(update.getCronExpression());
        if (update.getDurationMin() != null) rule.setDurationMin(update.getDurationMin());
        return ruleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    @Transactional
    public void toggleRule(Long ruleId) {
        IrrigationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RULE_NOT_FOUND", "规则不存在"));
        rule.setEnabled(!rule.getEnabled());
        ruleRepository.save(rule);
    }
}
