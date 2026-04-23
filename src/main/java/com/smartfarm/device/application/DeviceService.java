package com.smartfarm.device.application;

import com.smartfarm.device.domain.Device;
import com.smartfarm.device.domain.DeviceRepository;
import com.smartfarm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public List<Device> listDevices(Long tenantId, Device.DeviceType type, Device.DeviceStatus status) {
        if (type != null) return deviceRepository.findByTenantIdAndDeviceType(tenantId, type);
        if (status != null) return deviceRepository.findByTenantIdAndStatus(tenantId, status);
        return deviceRepository.findByTenantId(tenantId);
    }

    public Device getDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "设备不存在: " + deviceId));
    }

    @Transactional
    public Device registerDevice(Long tenantId, String deviceId, Device.DeviceType type,
                                  String name, String location, String greenhouseNo) {
        if (deviceRepository.existsByDeviceId(deviceId)) {
            throw new BusinessException("设备ID已存在: " + deviceId);
        }
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setDeviceId(deviceId);
        device.setDeviceType(type);
        device.setName(name);
        device.setLocation(location);
        device.setGreenhouseNo(greenhouseNo);
        device.setDeviceSecretEncrypted(new byte[0]); // TODO: 生成并加密设备密钥
        return deviceRepository.save(device);
    }

    @Transactional
    public void deleteDevice(String deviceId) {
        Device device = getDevice(deviceId);
        deviceRepository.delete(device);
    }

    @Transactional
    public void updateStatus(String deviceId, Device.DeviceStatus status) {
        Device device = getDevice(deviceId);
        if (status == Device.DeviceStatus.ONLINE) {
            device.goOnline();
        } else {
            device.goOffline();
        }
        deviceRepository.save(device);
    }

    @Transactional
    public Device updateDevice(String deviceId, String name, String location, String greenhouseNo) {
        Device device = getDevice(deviceId);
        if (name != null) device.setName(name);
        if (location != null) device.setLocation(location);
        if (greenhouseNo != null) device.setGreenhouseNo(greenhouseNo);
        return deviceRepository.save(device);
    }

    public List<Device> listByGreenhouse(Long tenantId, String greenhouseNo) {
        return deviceRepository.findByTenantIdAndGreenhouseNo(tenantId, greenhouseNo);
    }
}
