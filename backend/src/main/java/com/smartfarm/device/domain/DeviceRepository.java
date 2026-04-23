package com.smartfarm.device.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    List<Device> findByTenantId(Long tenantId);

    List<Device> findByTenantIdAndDeviceType(Long tenantId, Device.DeviceType type);

    List<Device> findByTenantIdAndStatus(Long tenantId, Device.DeviceStatus status);

    boolean existsByDeviceId(String deviceId);

    List<Device> findByTenantIdAndGreenhouseNo(Long tenantId, String greenhouseNo);

    List<Device> findByStatusAndLastOnlineAtBefore(Device.DeviceStatus status, java.time.OffsetDateTime before);
}
