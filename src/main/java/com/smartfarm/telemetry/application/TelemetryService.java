package com.smartfarm.telemetry.application;

import com.smartfarm.telemetry.domain.SensorData;
import com.smartfarm.telemetry.domain.SensorDataRepository;
import com.smartfarm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final SensorDataRepository sensorDataRepository;

    public SensorData getLatest(String deviceId) {
        return sensorDataRepository.findLatestByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_DATA", "暂无数据: " + deviceId));
    }

    public List<SensorData> getHistory(String deviceId, OffsetDateTime start, OffsetDateTime end) {
        return sensorDataRepository.findHistory(deviceId, start, end);
    }

    public List<SensorData> getRecentByTenant(Long tenantId, int minutes) {
        return sensorDataRepository.findRecentByTenant(tenantId, OffsetDateTime.now().minusMinutes(minutes));
    }

    public void ingest(SensorData data) {
        sensorDataRepository.save(data);
    }
}
