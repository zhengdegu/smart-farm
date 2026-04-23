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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备注册、查询、更新、删除")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @Operation(summary = "设备列表", description = "按类型/状态筛选设备")
    public List<Device> list(
            @RequestParam(required = false) Device.DeviceType type,
            @RequestParam(required = false) Device.DeviceStatus status) {
        return deviceService.listDevices(SecurityUtils.currentTenantId(), type, status);
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "设备详情")
    public Device get(@PathVariable String deviceId) {
        return deviceService.getDevice(deviceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "注册设备")
    public Device register(@Valid @RequestBody CreateDeviceRequest req) {
        return deviceService.registerDevice(
                SecurityUtils.currentTenantId(),
                req.getDeviceId(), req.getDeviceType(),
                req.getName(), req.getLocation(), req.getGreenhouseNo());
    }

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除设备")
    public void delete(@PathVariable String deviceId) {
        deviceService.deleteDevice(deviceId);
    }

    @PutMapping("/{deviceId}")
    @Operation(summary = "更新设备信息")
    public Device update(@PathVariable String deviceId, @RequestBody UpdateDeviceRequest req) {
        return deviceService.updateDevice(deviceId, req.getName(), req.getLocation(), req.getGreenhouseNo());
    }

    @GetMapping("/greenhouse/{greenhouseNo}")
    @Operation(summary = "按大棚查询设备")
    public List<Device> listByGreenhouse(@PathVariable String greenhouseNo) {
        return deviceService.listByGreenhouse(SecurityUtils.currentTenantId(), greenhouseNo);
    }

    @Data
    public static class CreateDeviceRequest {
        @NotBlank private String deviceId;
        @NotNull private Device.DeviceType deviceType;
        @NotBlank private String name;
        private String location;
        private String greenhouseNo;
    }

    @Data
    public static class UpdateDeviceRequest {
        private String name;
        private String location;
        private String greenhouseNo;
    }
}
