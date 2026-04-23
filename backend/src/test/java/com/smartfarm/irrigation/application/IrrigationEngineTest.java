package com.smartfarm.irrigation.application;

import com.smartfarm.alert.application.AlertService;
import com.smartfarm.irrigation.domain.*;
import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IrrigationEngineTest {

    @InjectMocks private IrrigationEngine engine;
    @Mock private RuleService ruleService;
    @Mock private CommandService commandService;
    @Mock private TelemetryService telemetryService;
    @Mock private AlertService alertService;
    @Mock private CommandLogRepository commandLogRepo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(engine, "commandTimeoutSeconds", 30);
        ReflectionTestUtils.setField(engine, "maxRetry", 3);
        ReflectionTestUtils.setField(engine, "maxDurationMinutes", 60);
    }

    @Test
    void evaluateThresholdRules_lowMoisture_opensValve() {
        IrrigationRule rule = buildRule(40.0, 80.0, "valve_001", "sensor_001");
        when(ruleService.getEnabledRules()).thenReturn(List.of(rule));

        SensorData data = new SensorData();
        data.setTime(OffsetDateTime.now());
        data.setValue(35.0);
        data.setDataType("soil_moisture");
        when(telemetryService.getLatest("sensor_001")).thenReturn(data);
        when(commandLogRepo.findByDeviceIdAndStatusIn(eq("valve_001"), any())).thenReturn(List.of());

        engine.evaluateThresholdRules();

        verify(commandService).sendCommand(eq(1L), eq("valve_001"),
                eq(CommandLog.CommandAction.OPEN_VALVE), any(), contains("AUTO:rule_"));
    }

    @Test
    void evaluateThresholdRules_highMoisture_closesValve() {
        IrrigationRule rule = buildRule(40.0, 80.0, "valve_001", "sensor_001");
        when(ruleService.getEnabledRules()).thenReturn(List.of(rule));

        SensorData data = new SensorData();
        data.setTime(OffsetDateTime.now());
        data.setValue(85.0);
        data.setDataType("soil_moisture");
        when(telemetryService.getLatest("sensor_001")).thenReturn(data);

        CommandLog activeCmd = new CommandLog();
        activeCmd.setAction(CommandLog.CommandAction.OPEN_VALVE);
        when(commandLogRepo.findByDeviceIdAndStatusIn(eq("valve_001"), any())).thenReturn(List.of(activeCmd));

        engine.evaluateThresholdRules();

        verify(commandService).sendCommand(eq(1L), eq("valve_001"),
                eq(CommandLog.CommandAction.CLOSE_VALVE), any(), contains("AUTO:rule_"));
    }

    @Test
    void evaluateThresholdRules_normalMoisture_noAction() {
        IrrigationRule rule = buildRule(40.0, 80.0, "valve_001", "sensor_001");
        when(ruleService.getEnabledRules()).thenReturn(List.of(rule));

        SensorData data = new SensorData();
        data.setTime(OffsetDateTime.now());
        data.setValue(60.0);
        data.setDataType("soil_moisture");
        when(telemetryService.getLatest("sensor_001")).thenReturn(data);

        engine.evaluateThresholdRules();

        verifyNoInteractions(commandService);
    }

    @Test
    void evaluateThresholdRules_staleData_skipped() {
        IrrigationRule rule = buildRule(40.0, 80.0, "valve_001", "sensor_001");
        when(ruleService.getEnabledRules()).thenReturn(List.of(rule));

        SensorData data = new SensorData();
        data.setTime(OffsetDateTime.now().minusMinutes(15));
        data.setValue(30.0);
        data.setDataType("soil_moisture");
        when(telemetryService.getLatest("sensor_001")).thenReturn(data);

        engine.evaluateThresholdRules();

        verifyNoInteractions(commandService);
    }

    @Test
    void enforceSafetyGuard_overdueOpenValve_forcesClose() {
        CommandLog cmd = new CommandLog();
        cmd.setCmdId("cmd_test");
        cmd.setTenantId(1L);
        cmd.setDeviceId("valve_001");
        cmd.setAction(CommandLog.CommandAction.OPEN_VALVE);
        when(commandLogRepo.findByStatusInAndCreatedAtBefore(any(), any())).thenReturn(List.of(cmd));

        engine.enforceSafetyGuard();

        verify(commandService).sendCommand(eq(1L), eq("valve_001"),
                eq(CommandLog.CommandAction.CLOSE_VALVE), any(), eq("SYSTEM:safety_guard"));
        verify(alertService).createAlert(eq(1L), eq("valve_001"),
                eq(com.smartfarm.alert.domain.Alert.AlertLevel.L1), eq("SAFETY_GUARD"), any(), any());
    }

    @Test
    void checkCommandTimeout_withinRetry_retriesCommand() {
        CommandLog cmd = new CommandLog();
        cmd.setCmdId("cmd_timeout");
        cmd.setTenantId(1L);
        cmd.setDeviceId("valve_001");
        cmd.setAction(CommandLog.CommandAction.OPEN_VALVE);
        cmd.setRetryCount(1);
        cmd.setTriggeredBy("AUTO:rule_1");
        when(commandLogRepo.findByStatusAndSentAtBefore(any(), any())).thenReturn(List.of(cmd));

        engine.checkCommandTimeout();

        verify(commandLogRepo).save(cmd);
        verify(commandService).sendCommand(eq(1L), eq("valve_001"),
                eq(CommandLog.CommandAction.OPEN_VALVE), any(), contains("retry"));
    }

    @Test
    void checkCommandTimeout_maxRetryExceeded_marksFailed() {
        CommandLog cmd = new CommandLog();
        cmd.setCmdId("cmd_final");
        cmd.setTenantId(1L);
        cmd.setDeviceId("valve_001");
        cmd.setAction(CommandLog.CommandAction.OPEN_VALVE);
        cmd.setRetryCount(3);
        when(commandLogRepo.findByStatusAndSentAtBefore(any(), any())).thenReturn(List.of(cmd));

        engine.checkCommandTimeout();

        verify(commandLogRepo).save(cmd);
        verify(alertService).createAlert(eq(1L), eq("valve_001"),
                any(), eq("CMD_TIMEOUT"), any(), any());
        verifyNoInteractions(commandService);
    }

    private IrrigationRule buildRule(Double low, Double high, String deviceId, String sensorId) {
        IrrigationRule rule = new IrrigationRule();
        ReflectionTestUtils.setField(rule, "id", 1L);
        rule.setTenantId(1L);
        rule.setName("test-rule");
        rule.setRuleType(IrrigationRule.RuleType.THRESHOLD);
        rule.setDeviceId(deviceId);
        rule.setSensorDeviceId(sensorId);
        rule.setThresholdLow(low);
        rule.setThresholdHigh(high);
        rule.setDurationMin(30);
        rule.setEnabled(true);
        return rule;
    }
}
