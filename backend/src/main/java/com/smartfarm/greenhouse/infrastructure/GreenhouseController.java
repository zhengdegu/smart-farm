package com.smartfarm.greenhouse.infrastructure;

import com.smartfarm.device.domain.Device;
import com.smartfarm.greenhouse.application.GreenhouseService;
import com.smartfarm.greenhouse.domain.Greenhouse;
import com.smartfarm.greenhouse.domain.Greenhouse.GreenhouseStatus;
import com.smartfarm.shared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/greenhouses")
@RequiredArgsConstructor
@Tag(name = "大棚管理", description = "大棚增删改查、关联设备、环境数据")
public class GreenhouseController {

    private final GreenhouseService greenhouseService;

    @GetMapping
    @Operation(summary = "大棚列表", description = "按状态筛选大棚")
    public List<Greenhouse> list(@RequestParam(required = false) GreenhouseStatus status) {
        return greenhouseService.list(SecurityUtils.currentTenantId(), status);
    }

    @GetMapping("/stats")
    @Operation(summary = "大棚统计")
    public Map<String, Long> stats() {
        return greenhouseService.getStats(SecurityUtils.currentTenantId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "大棚详情")
    public Greenhouse get(@PathVariable Long id) {
        return greenhouseService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增大棚")
    public Greenhouse create(@Valid @RequestBody CreateGreenhouseRequest req) {
        return greenhouseService.create(
                SecurityUtils.currentTenantId(),
                req.getName(), req.getGreenhouseNo(),
                req.getArea(), req.getLocation(), req.getDescription());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新大棚")
    public Greenhouse update(@PathVariable Long id, @Valid @RequestBody UpdateGreenhouseRequest req) {
        return greenhouseService.update(id,
                req.getName(), req.getArea(), req.getLocation(),
                req.getDescription(), req.getStatus());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除大棚")
    public void delete(@PathVariable Long id) {
        greenhouseService.delete(id);
    }

    @GetMapping("/{id}/devices")
    @Operation(summary = "大棚关联设备")
    public List<Device> listDevices(@PathVariable Long id) {
        return greenhouseService.listDevices(id);
    }

    @GetMapping("/{id}/environment")
    @Operation(summary = "大棚环境数据")
    public Map<String, Object> getEnvironment(@PathVariable Long id) {
        return greenhouseService.getEnvironment(id);
    }

    @Data
    public static class CreateGreenhouseRequest {
        @NotBlank private String name;
        @NotBlank private String greenhouseNo;
        private Double area;
        private String location;
        private String description;
    }

    @Data
    public static class UpdateGreenhouseRequest {
        @NotBlank private String name;
        private Double area;
        private String location;
        private String description;
        private GreenhouseStatus status;
    }
}
