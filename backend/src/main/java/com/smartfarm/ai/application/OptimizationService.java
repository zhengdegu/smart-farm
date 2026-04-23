package com.smartfarm.ai.application;

import com.smartfarm.ai.domain.*;
import com.smartfarm.irrigation.domain.IrrigationRule;
import com.smartfarm.irrigation.domain.IrrigationRuleRepository;
import com.smartfarm.report.application.ReportService;
import com.smartfarm.report.domain.IrrigationDailySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * AI 自主决策引擎 (L3) — 基于历史数据自动优化灌溉策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationService {

    private final LlmClient llmClient;
    private final OptimizationSuggestionRepository suggestionRepo;
    private final IrrigationRuleRepository ruleRepo;
    private final ReportService reportService;

    private static final Long DEFAULT_TENANT = 1L;

    /**
     * 每周一凌晨3点分析历史数据，生成优化建议
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void analyzeAndSuggest() {
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);

            List<IrrigationDailySummary> stats = reportService.getDailyStats(DEFAULT_TENANT, start, end, null);
            if (stats.size() < 14) {
                log.info("数据不足14天，跳过优化分析");
                return;
            }

            // 按棚号分组分析
            Map<String, List<IrrigationDailySummary>> byGreenhouse = new LinkedHashMap<>();
            for (var s : stats) {
                byGreenhouse.computeIfAbsent(s.getGreenhouseNo(), k -> new ArrayList<>()).add(s);
            }

            for (var entry : byGreenhouse.entrySet()) {
                String ghNo = entry.getKey();
                List<IrrigationDailySummary> ghStats = entry.getValue();

                // 统计分析
                double avgMoisture = ghStats.stream()
                        .filter(s -> s.getAvgSoilMoisture() != null)
                        .mapToDouble(s -> s.getAvgSoilMoisture().doubleValue())
                        .average().orElse(0);
                double avgWater = ghStats.stream().mapToDouble(IrrigationDailySummary::getEstimatedWaterLiters).average().orElse(0);
                int avgCount = (int) ghStats.stream().mapToInt(IrrigationDailySummary::getIrrigationCount).average().orElse(0);
                double autoRate = ghStats.stream().mapToInt(IrrigationDailySummary::getAutoTriggerCount).sum() * 100.0 /
                        Math.max(1, ghStats.stream().mapToInt(IrrigationDailySummary::getIrrigationCount).sum());

                // 获取当前规则
                List<IrrigationRule> rules = ruleRepo.findByTenantIdAndGreenhouseNo(DEFAULT_TENANT, ghNo);

                String context = String.format("""
                        棚号: %s
                        分析周期: %s ~ %s (%d天数据)
                        平均土壤湿度: %.1f%%
                        日均灌溉次数: %d
                        日均用水量: %.1f升
                        自动化率: %.1f%%
                        当前规则数: %d
                        当前阈值: %s
                        
                        请分析以上数据，给出灌溉策略优化建议。
                        如果数据正常无需调整，回复 "NO_SUGGESTION"。
                        如果有建议，请给出：
                        1. 建议类型（THRESHOLD/DURATION/SCHEDULE）
                        2. 当前值和建议值
                        3. 置信度（0-1）
                        4. 理由（一句话）
                        """,
                        ghNo, start, end, ghStats.size(), avgMoisture, avgCount, avgWater, autoRate,
                        rules.size(), rules.isEmpty() ? "无" : rules.get(0).getThresholdLow() + "-" + rules.get(0).getThresholdHigh());

                LlmClient.LlmResponse resp = llmClient.chat(
                        "你是灌溉策略优化专家，基于历史数据分析给出优化建议。", context);

                if (!resp.content().contains("NO_SUGGESTION")) {
                    OptimizationSuggestion suggestion = new OptimizationSuggestion();
                    suggestion.setTenantId(DEFAULT_TENANT);
                    suggestion.setGreenhouseNo(ghNo);
                    suggestion.setSuggestionType("THRESHOLD");
                    suggestion.setCurrentValue(String.format("{\"avgMoisture\":%.1f,\"avgWater\":%.1f,\"avgCount\":%d}", avgMoisture, avgWater, avgCount));
                    suggestion.setSuggestedValue(resp.content().length() > 500 ? resp.content().substring(0, 500) : resp.content());
                    suggestion.setConfidence(0.7);
                    suggestion.setReasoning(resp.content());
                    suggestion.setStatus(OptimizationSuggestion.SuggestionStatus.PENDING);
                    suggestionRepo.save(suggestion);

                    log.info("生成优化建议: ghNo={}, type=THRESHOLD", ghNo);
                }
            }
        } catch (Exception e) {
            log.warn("优化分析失败: {}", e.getMessage());
        }
    }

    public List<OptimizationSuggestion> listSuggestions(Long tenantId) {
        return suggestionRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<OptimizationSuggestion> listPending(Long tenantId) {
        return suggestionRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, OptimizationSuggestion.SuggestionStatus.PENDING);
    }

    public OptimizationSuggestion accept(Long id, Long userId) {
        OptimizationSuggestion s = suggestionRepo.findById(id).orElseThrow(() -> new RuntimeException("建议不存在"));
        s.setStatus(OptimizationSuggestion.SuggestionStatus.ACCEPTED);
        s.setAcceptedBy(userId);
        return suggestionRepo.save(s);
    }

    public OptimizationSuggestion reject(Long id) {
        OptimizationSuggestion s = suggestionRepo.findById(id).orElseThrow(() -> new RuntimeException("建议不存在"));
        s.setStatus(OptimizationSuggestion.SuggestionStatus.REJECTED);
        return suggestionRepo.save(s);
    }
}
