package com.smartfarm.device.application;

import com.smartfarm.alert.application.AlertService;
import com.smartfarm.alert.domain.Alert;
import com.smartfarm.device.domain.Device;
import com.smartfarm.device.domain.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 设备离线检测 - 每2分钟检查一次
 * 在线设备超过5分钟无心跳 → 标记离线 + L2告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHealthChecker {

    private final DeviceRepository deviceRepo;
    private final AlertService alertService;

    @Scheduled(fixedDelay = 120000)
    public void checkOfflineDevices() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(5);
        List<Device> stale = deviceRepo.findByStatusAndLastOnlineAtBefore(Device.DeviceStatus.ONLINE, cutoff);

        for (Device device : stale) {
            log.warn("设备离线: {} (最后在线: {})", device.getDeviceId(), device.getLastOnlineAt());
            device.goOffline();
            deviceRepo.save(device);

            alertService.createAlert(device.getTenantId(), device.getDeviceId(),
                    Alert.AlertLevel.L2, "DEVICE_OFFLINE",
                    "设备离线: " + device.getName(),
                    "设备 " + device.getDeviceId() + " 已超过5分钟无心跳");
        }

        if (!stale.isEmpty()) {
            log.info("设备离线检测: {} 台设备标记为离线", stale.size());
        }
    }
}
