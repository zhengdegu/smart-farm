package com.smartfarm.greenhouse.application;

import com.smartfarm.device.application.DeviceService;
import com.smartfarm.device.domain.Device;
import com.smartfarm.greenhouse.domain.Greenhouse;
import com.smartfarm.greenhouse.domain.Greenhouse.GreenhouseStatus;
import com.smartfarm.greenhouse.domain.GreenhouseRepository;
import com.smartfarm.shared.exception.BusinessException;
import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GreenhouseService {

    private final GreenhouseRepository greenhouseRepository;
    private final DeviceService deviceService;
    private final TelemetryService telemetryService;

    public List<Greenhouse> list(Long tenantId, GreenhouseStatus status) {
        if (status != null) {
            return greenhouseRepository.findByTenantIdAndStatus(tenantId, status);
        }
        return greenhouseRepository.findByTenantId(tenantId);
    }

    public Greenhouse getById(Long id) {
        return greenhouseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "GREENHOUSE_NOT_FOUND", "大棚不存在: " + id));
    }

    @Transactional
    public Greenhouse create(Long tenantId, String name, String greenhouseNo,
                             Double area, String location, String description) {
        if (greenhouseRepository.existsByTenantIdAndGreenhouseNo(tenantId, greenhouseNo)) {
            throw new BusinessException("该租户下棚号已存在: " + greenhouseNo);
        }
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setTenantId(tenantId);
        greenhouse.setName(name);
        greenhouse.setGreenhouseNo(greenhouseNo);
        greenhouse.setArea(area);
        greenhouse.setLocation(location);
        greenhouse.setDescription(description);
        greenhouse.setStatus(GreenhouseStatus.ACTIVE);
        return greenhouseRepository.save(greenhouse);
    }

    @Transactional
    public Greenhouse update(Long id, String name, Double area, String location,
                             String description, GreenhouseStatus status) {
        Greenhouse greenhouse = getById(id);
        if (name != null) greenhouse.setName(name);
        if (area != null) greenhouse.setArea(area);
        if (location != null) greenhouse.setLocation(location);
        if (description != null) greenhouse.setDescription(description);
        if (status != null) greenhouse.setStatus(status);
        return greenhouseRepository.save(greenhouse);
    }

    @Transactional
    public void delete(Long id) {
        greenhouseRepository.deleteById(id);
    }

    public Map<String, Long> getStats(Long tenantId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", greenhouseRepository.countByTenantId(tenantId));
        stats.put("active", greenhouseRepository.countByTenantIdAndStatus(tenantId, GreenhouseStatus.ACTIVE));
        stats.put("idle", greenhouseRepository.countByTenantIdAndStatus(tenantId, GreenhouseStatus.IDLE));
        return stats;
    }

    public List<Device> listDevices(Long id) {
        Greenhouse greenhouse = getById(id);
        return deviceService.listByGreenhouse(greenhouse.getTenantId(), greenhouse.getGreenhouseNo());
    }

    public Map<String, Object> getEnvironment(Long id) {
        List<Device> devices = listDevices(id);
        List<Device> sensors = devices.stream()
                .filter(d -> d.getDeviceType() == Device.DeviceType.SENSOR)
                .toList();

        Map<String, Object> environment = new HashMap<>();
        for (Device sensor : sensors) {
            try {
                SensorData data = telemetryService.getLatest(sensor.getDeviceId());
                if (data != null && data.getDataType() != null) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("value", data.getValue());
                    entry.put("unit", data.getUnit());
                    entry.put("deviceName", sensor.getName());
                    entry.put("time", data.getTime());
                    environment.put(data.getDataType(), entry);
                }
            } catch (BusinessException ignored) {
                // No data for this sensor, skip
            }
        }
        return environment;
    }
}
