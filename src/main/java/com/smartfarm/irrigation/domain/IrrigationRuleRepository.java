package com.smartfarm.irrigation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IrrigationRuleRepository extends JpaRepository<IrrigationRule, Long> {

    List<IrrigationRule> findByTenantId(Long tenantId);

    List<IrrigationRule> findByEnabledTrue();

    List<IrrigationRule> findByDeviceId(String deviceId);
}
