package com.smartfarm.device.infrastructure;

import com.smartfarm.device.application.DeviceService;
import com.smartfarm.device.domain.Device;
import com.smartfarm.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<Device> list(
            @RequestParam(required = false) Device.DeviceType type,
            @RequestParam(required = false) Device.DeviceStatus status) {
        return deviceService.listDevices(SecurityUtils.currentTenantId(), type, status);
    }

    @GetMapping("/{deviceId}")
    public Device get(@PathVariable String deviceId) {
        return deviceService.getDevice(deviceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Device register(@Valid @RequestBody CreateDeviceRequest req) {
        return deviceService.registerDevice(
                SecurityUtils.currentTenantId(),
                req.getDeviceId(), req.getDeviceType(),
                req.getName(), req.getLocation(), req.getGreenhouseNo());
    }

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String deviceId) {
        deviceService.deleteDevice(deviceId);
    }

    @Data
    public static class CreateDeviceRequest {
        @NotBlank private String deviceId;
        @NotNull private Device.DeviceType deviceType;
        @NotBlank private String name;
        private String location;
        private String greenhouseNo;
    }
}
