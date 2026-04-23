package com.smartfarm.report.application;

import com.smartfarm.alert.domain.AlertRepository;
import com.smartfarm.device.domain.DeviceRepository;
import com.smartfarm.irrigation.domain.CommandLog;
import com.smartfarm.irrigation.domain.CommandLogRepository;
import com.smartfarm.report.domain.*;
import com.smartfarm.telemetry.domain.SensorDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks private ReportService reportService;
    @Mock private IrrigationDailySummaryRepository dailyRepo;
    @Mock private MonthlyReportRepository monthlyRepo;
    @Mock private CommandLogRepository commandLogRepo;
    @Mock private DeviceRepository deviceRepo;
    @Mock private AlertRepository alertRepo;

    @Test
    void getDailyStats_withGreenhouseNo_filtersCorrectly() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 7);
        IrrigationDailySummary s = new IrrigationDailySummary();
        s.setTenantId(1L);
        s.setGreenhouseNo("GH01");
        s.setSummaryDate(start);
        s.setIrrigationCount(5);

        when(dailyRepo.findByTenantIdAndGreenhouseNoAndSummaryDateBetween(1L, "GH01", start, end))
                .thenReturn(List.of(s));

        List<IrrigationDailySummary> result = reportService.getDailyStats(1L, start, end, "GH01");

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getIrrigationCount());
        verify(dailyRepo).findByTenantIdAndGreenhouseNoAndSummaryDateBetween(1L, "GH01", start, end);
    }

    @Test
    void getDailyStats_withoutGreenhouseNo_returnsAll() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 7);

        when(dailyRepo.findByTenantIdAndSummaryDateBetween(1L, start, end)).thenReturn(List.of());

        List<IrrigationDailySummary> result = reportService.getDailyStats(1L, start, end, null);

        assertEquals(0, result.size());
        verify(dailyRepo).findByTenantIdAndSummaryDateBetween(1L, start, end);
    }

    @Test
    void getMonthlyStats_aggregatesCorrectly() {
        LocalDate month = LocalDate.of(2026, 4, 15);
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);

        IrrigationDailySummary s1 = buildSummary(3, 90, 900.0, 2, 1);
        IrrigationDailySummary s2 = buildSummary(5, 150, 1500.0, 4, 1);

        when(dailyRepo.findByTenantIdAndSummaryDateBetween(1L, start, end)).thenReturn(List.of(s1, s2));

        Map<String, Object> result = reportService.getMonthlyStats(1L, month);

        assertEquals(8, result.get("totalIrrigationCount"));
        assertEquals(240, result.get("totalDurationMin"));
        assertEquals(2400.0, result.get("totalWaterLiters"));
        assertEquals(6, result.get("autoTriggerCount"));
        assertEquals(2, result.get("manualTriggerCount"));
        assertEquals(75.0, result.get("autoRate"));
    }

    @Test
    void getWaterSavingComparison_calculatesCorrectly() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 10);

        when(dailyRepo.sumWaterByTenantAndDateRange(1L, start, end)).thenReturn(700.0);

        Map<String, Object> result = reportService.getWaterSavingComparison(1L, start, end, 100.0);

        assertEquals(700.0, result.get("systemWaterLiters"));
        assertEquals(1000.0, result.get("manualEstimateLiters"));
        assertEquals(30.0, result.get("waterSavedPercent"));
    }

    @Test
    void getWaterSavingComparison_noSystemWater_returns100Percent() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 10);

        when(dailyRepo.sumWaterByTenantAndDateRange(1L, start, end)).thenReturn(null);

        Map<String, Object> result = reportService.getWaterSavingComparison(1L, start, end, 100.0);

        assertEquals(0.0, result.get("systemWaterLiters"));
        assertEquals(100.0, result.get("waterSavedPercent"));
    }

    @Test
    void getAlertStats_aggregatesByLevelAndType() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);

        List<Object[]> byLevel = List.of(new Object[]{"L1", 3L}, new Object[]{"L2", 8L});
        List<Object[]> byType = List.of(new Object[]{"DEVICE_OFFLINE", 5L}, new Object[]{"MOISTURE_LOW", 6L});

        when(alertRepo.countByLevelAndDateRange(eq(1L), any(), any())).thenReturn(byLevel);
        when(alertRepo.countByTypeAndDateRange(eq(1L), any(), any())).thenReturn(byType);

        Map<String, Object> result = reportService.getAlertStats(1L, start, end);

        @SuppressWarnings("unchecked")
        Map<String, Long> levelMap = (Map<String, Long>) result.get("byLevel");
        assertEquals(3L, levelMap.get("L1"));
        assertEquals(8L, levelMap.get("L2"));
        assertEquals(11L, result.get("total"));
    }

    private IrrigationDailySummary buildSummary(int count, int duration, double water, int auto, int manual) {
        IrrigationDailySummary s = new IrrigationDailySummary();
        s.setTenantId(1L);
        s.setIrrigationCount(count);
        s.setTotalDurationMin(duration);
        s.setEstimatedWaterLiters(water);
        s.setAutoTriggerCount(auto);
        s.setManualTriggerCount(manual);
        return s;
    }
}
