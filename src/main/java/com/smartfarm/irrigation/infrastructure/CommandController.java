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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/commands")
@RequiredArgsConstructor
public class CommandController {

    private final CommandService commandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandLog send(@Valid @RequestBody CreateCommandRequest req) {
        return commandService.sendCommand(
                SecurityUtils.currentTenantId(),
                req.getDeviceId(), req.getAction(),
                req.getParams(),
                "USER:" + SecurityUtils.currentUserId());
    }

    @GetMapping("/{cmdId}")
    public CommandLog get(@PathVariable String cmdId) {
        return commandService.getCommand(cmdId);
    }

    @PostMapping("/emergency-stop")
    public Map<String, Object> emergencyStop() {
        int count = commandService.emergencyStop(SecurityUtils.currentTenantId());
        return Map.of("stopped_count", count);
    }

    @GetMapping("/history")
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
