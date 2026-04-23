package com.smartfarm.irrigation.infrastructure;

import com.smartfarm.irrigation.application.CommandService;
import com.smartfarm.irrigation.domain.CommandLog;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/commands")
@RequiredArgsConstructor
@Tag(name = "灌溉指令", description = "发送指令、紧急停止、历史记录")
public class CommandController {

    private final CommandService commandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "发送灌溉指令")
    public CommandLog send(@Valid @RequestBody CreateCommandRequest req) {
        return commandService.sendCommand(
                SecurityUtils.currentTenantId(),
                req.getDeviceId(), req.getAction(),
                req.getParams(),
                "USER:" + SecurityUtils.currentUserId());
    }

    @GetMapping("/{cmdId}")
    @Operation(summary = "查询指令状态")
    public CommandLog get(@PathVariable String cmdId) {
        return commandService.getCommand(cmdId);
    }

    @PostMapping("/emergency-stop")
    @Operation(summary = "紧急停止所有灌溉")
    public Map<String, Object> emergencyStop() {
        int count = commandService.emergencyStop(SecurityUtils.currentTenantId());
        return Map.of("stopped_count", count);
    }

    @GetMapping("/history")
    @Operation(summary = "指令历史记录")
    public List<CommandLog> history() {
        return commandService.listCommands(SecurityUtils.currentTenantId());
    }

    @Data
    public static class CreateCommandRequest {
        @NotBlank private String deviceId;
        @NotNull private CommandLog.CommandAction action;
        private Map<String, Object> params;
    }
}
