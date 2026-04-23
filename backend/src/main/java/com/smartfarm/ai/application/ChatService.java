package com.smartfarm.ai.application;

import com.smartfarm.ai.domain.*;
import com.smartfarm.alert.application.AlertService;
import com.smartfarm.crop.application.GreenhousePlantingService;
import com.smartfarm.device.application.DeviceService;
import com.smartfarm.irrigation.application.CommandService;
import com.smartfarm.irrigation.domain.CommandLog;
import com.smartfarm.report.application.ReportService;
import com.smartfarm.shared.application.WeatherService;
import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmClient llmClient;
    private final ConversationRepository conversationRepo;
    private final TelemetryService telemetryService;
    private final CommandService commandService;
    private final AlertService alertService;
    private final ReportService reportService;
    private final DeviceService deviceService;
    private final GreenhousePlantingService plantingService;
    private final WeatherService weatherService;

    private static final String SYSTEM_PROMPT = """
            你是"小农"，智慧农业平台的AI助手。你帮助农户管理温室灌溉。
            
            你的能力：
            1. 查询传感器数据（土壤湿度、温度等）
            2. 控制灌溉阀门（开/关/紧急停止）
            3. 查看告警信息
            4. 查看灌溉统计和节水报表
            5. 查看作物生长阶段和灌溉建议
            6. 查看天气信息
            
            沟通风格：
            - 用农户听得懂的语言，不用技术术语
            - 直接说结论和建议
            - 给出具体数字（"建议灌溉20分钟"而不是"适当灌溉"）
            - 紧急情况用简短有力的语言
            """;

    private static final List<LlmClient.FunctionDef> FUNCTIONS = List.of(
            new LlmClient.FunctionDef("get_sensor_data", "获取传感器最新数据",
                    Map.of("type", "object", "properties", Map.of(
                            "device_id", Map.of("type", "string", "description", "传感器设备ID，如 sensor_001")
                    ))),
            new LlmClient.FunctionDef("control_valve", "控制灌溉阀门",
                    Map.of("type", "object", "properties", Map.of(
                            "device_id", Map.of("type", "string", "description", "阀门设备ID"),
                            "action", Map.of("type", "string", "enum", List.of("OPEN_VALVE", "CLOSE_VALVE")),
                            "duration_min", Map.of("type", "integer", "description", "灌溉时长(分钟)")
                    ))),
            new LlmClient.FunctionDef("emergency_stop", "紧急停止所有灌溉", Map.of("type", "object", "properties", Map.of())),
            new LlmClient.FunctionDef("get_alerts", "获取告警列表",
                    Map.of("type", "object", "properties", Map.of(
                            "level", Map.of("type", "string", "description", "告警级别: L1/L2/L3/ALL")
                    ))),
            new LlmClient.FunctionDef("get_irrigation_stats", "获取灌溉统计",
                    Map.of("type", "object", "properties", Map.of(
                            "period", Map.of("type", "string", "description", "统计周期: today/week/month")
                    ))),
            new LlmClient.FunctionDef("get_weather", "获取当前天气和预报", Map.of("type", "object", "properties", Map.of())),
            new LlmClient.FunctionDef("get_crop_stage", "获取作物生长阶段信息",
                    Map.of("type", "object", "properties", Map.of(
                            "greenhouse_no", Map.of("type", "string", "description", "棚号如GH01")
                    )))
    );

    public Map<String, Object> chat(Long tenantId, Long userId, String sessionId, String userMessage) {
        // 保存用户消息
        saveConversation(tenantId, userId, sessionId, "user", userMessage, null, null);

        // 获取历史对话（最近10条）
        List<Conversation> history = conversationRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Map<String, String>> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            Conversation c = history.get(i);
            messages.add(Map.of("role", c.getRole(), "content", c.getContent()));
        }

        // 调用大模型
        LlmClient.LlmResponse resp = llmClient.chat(SYSTEM_PROMPT, messages, FUNCTIONS);

        String reply;
        if (resp.functionName() != null) {
            // 执行 Function Calling
            String funcResult = executeFunction(tenantId, resp.functionName(), resp.functionArgs());

            // 把 function 结果再发给大模型生成最终回复
            messages.add(Map.of("role", "assistant", "content", ""));
            messages.add(Map.of("role", "user", "content", "函数执行结果: " + funcResult + "\n请根据以上数据用通俗语言回复用户。"));
            LlmClient.LlmResponse finalResp = llmClient.chat(SYSTEM_PROMPT, messages, null);
            reply = finalResp.content();
        } else {
            reply = resp.content();
        }

        // 保存 AI 回复
        saveConversation(tenantId, userId, sessionId, "assistant", reply, resp.model(), resp.tokensUsed());

        return Map.of(
                "reply", reply,
                "sessionId", sessionId,
                "model", resp.model() != null ? resp.model() : "unknown",
                "tokensUsed", resp.tokensUsed(),
                "latencyMs", resp.latencyMs()
        );
    }

    private String executeFunction(Long tenantId, String funcName, Map<String, Object> args) {
        try {
            return switch (funcName) {
                case "get_sensor_data" -> {
                    String deviceId = (String) args.getOrDefault("device_id", "sensor_001");
                    SensorData data = telemetryService.getLatest(deviceId);
                    yield data != null ? String.format("设备%s: %s=%.1f, 时间=%s", deviceId, data.getDataType(), data.getValue(), data.getTime()) : "暂无数据";
                }
                case "control_valve" -> {
                    String deviceId = (String) args.get("device_id");
                    String action = (String) args.get("action");
                    int duration = args.containsKey("duration_min") ? ((Number) args.get("duration_min")).intValue() : 30;                CommandLog cmd = commandService.sendCommand(tenantId, deviceId,
                            CommandLog.CommandAction.valueOf(action),
                            Map.of("duration_min", duration), "AI:chat");
                    yield String.format("指令已发送: %s %s, 时长%d分钟, 状态=%s", action, deviceId, duration, cmd.getStatus());
                }
                case "emergency_stop" -> {
                    int count = commandService.emergencyStop(tenantId);
                    yield "紧急停止已执行，关闭了 " + count + " 个阀门";
                }
                case "get_alerts" -> {
                    var stats = alertService.getStats(tenantId);
                    yield "告警统计: " + stats.toString();
                }
                case "get_irrigation_stats" -> {
                    String period = (String) args.getOrDefault("period", "week");
                    LocalDate end = LocalDate.now();
                    LocalDate start = switch (period) {
                        case "today" -> end;
                        case "month" -> end.minusDays(30);
                        default -> end.minusDays(7);
                    };
                    var stats = reportService.getDailyStats(tenantId, start, end, null);
                    int totalCount = stats.stream().mapToInt(s -> s.getIrrigationCount()).sum();
                    double totalWater = stats.stream().mapToDouble(s -> s.getEstimatedWaterLiters()).sum();
                    yield String.format("灌溉统计(%s): 共%d次, 用水%.1f升", period, totalCount, totalWater);
                }
                case "get_weather" -> {
                    yield weatherService.getWeatherDescription() + ", 灌溉调整系数=" + weatherService.getIrrigationAdjustmentFactor();
                }
                case "get_crop_stage" -> {
                    String ghNo = (String) args.getOrDefault("greenhouse_no", "GH01");
                    var plantings = plantingService.listByGreenhouse(ghNo);
                    if (plantings.isEmpty()) yield ghNo + "号棚暂无种植记录";
                    var info = plantingService.getStageInfo(plantings.get(0).getId());
                    yield ghNo + "号棚: " + info.toString();
                }
                default -> "未知功能: " + funcName;
            };
        } catch (Exception e) {
            log.error("Function执行失败: {}({})", funcName, args, e);
            return "操作失败: " + e.getMessage();
        }
    }

    private void saveConversation(Long tenantId, Long userId, String sessionId, String role, String content, String model, Integer tokens) {
        Conversation c = new Conversation();
        c.setTenantId(tenantId);
        c.setUserId(userId);
        c.setSessionId(sessionId);
        c.setRole(role);
        c.setContent(content);
        c.setModel(model);
        c.setTokensUsed(tokens);
        conversationRepo.save(c);
    }

    public List<Conversation> getHistory(String sessionId) {
        return conversationRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
