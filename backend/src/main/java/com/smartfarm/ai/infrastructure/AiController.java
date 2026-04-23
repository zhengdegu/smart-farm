package com.smartfarm.ai.infrastructure;

import com.smartfarm.ai.application.ChatService;
import com.smartfarm.ai.application.OptimizationService;
import com.smartfarm.ai.application.PatrolService;
import com.smartfarm.ai.domain.Conversation;
import com.smartfarm.ai.domain.OptimizationSuggestion;
import com.smartfarm.ai.domain.PatrolLog;
import com.smartfarm.shared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI智能体", description = "对话助手、巡检Agent、策略优化")
public class AiController {

    private final ChatService chatService;
    private final PatrolService patrolService;
    private final OptimizationService optimizationService;

    // ===== L1: 对话助手 =====

    @PostMapping("/chat")
    @Operation(summary = "AI对话", description = "发送消息，AI回复（支持Function Calling）")
    public Map<String, Object> chat(@RequestBody ChatRequest req) {
        String sessionId = req.getSessionId() != null ? req.getSessionId() : UUID.randomUUID().toString();
        return chatService.chat(SecurityUtils.currentTenantId(), SecurityUtils.currentUserId(), sessionId, req.getMessage());
    }

    @GetMapping("/chat/history")
    @Operation(summary = "对话历史")
    public List<Conversation> chatHistory(@RequestParam String sessionId) {
        return chatService.getHistory(sessionId);
    }

    // ===== L2: 巡检Agent =====

    @GetMapping("/patrol/logs")
    @Operation(summary = "巡检记录")
    public List<PatrolLog> patrolLogs() {
        return patrolService.listLogs(SecurityUtils.currentTenantId());
    }

    @PostMapping("/patrol/trigger")
    @Operation(summary = "手动触发巡检")
    public PatrolLog triggerPatrol(@RequestParam(defaultValue = "TREND_CHECK") String type) {
        return patrolService.triggerPatrol(SecurityUtils.currentTenantId(), type);
    }

    // ===== L3: 策略优化 =====

    @GetMapping("/suggestions")
    @Operation(summary = "优化建议列表")
    public List<OptimizationSuggestion> suggestions(@RequestParam(required = false) Boolean pendingOnly) {
        Long tenantId = SecurityUtils.currentTenantId();
        return Boolean.TRUE.equals(pendingOnly) ? optimizationService.listPending(tenantId) : optimizationService.listSuggestions(tenantId);
    }

    @PostMapping("/suggestions/{id}/accept")
    @Operation(summary = "接受优化建议")
    public OptimizationSuggestion acceptSuggestion(@PathVariable Long id) {
        return optimizationService.accept(id, SecurityUtils.currentUserId());
    }

    @PostMapping("/suggestions/{id}/reject")
    @Operation(summary = "拒绝优化建议")
    public OptimizationSuggestion rejectSuggestion(@PathVariable Long id) {
        return optimizationService.reject(id);
    }

    @Data
    public static class ChatRequest {
        private String message;
        private String sessionId;
    }
}
