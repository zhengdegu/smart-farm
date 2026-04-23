package com.smartfarm.irrigation.application;

import com.smartfarm.irrigation.domain.*;
import com.smartfarm.shared.mqtt.MqttPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @InjectMocks private CommandService commandService;
    @Mock private CommandLogRepository commandLogRepository;
    @Mock private MqttPublisher mqttPublisher;

    @Test
    void sendCommand_createsAndPublishes() {
        ReflectionTestUtils.setField(commandService, "defaultTenantId", "t001");
        when(commandLogRepository.save(any(CommandLog.class))).thenAnswer(i -> i.getArgument(0));

        CommandLog result = commandService.sendCommand(1L, "valve_001",
                CommandLog.CommandAction.OPEN_VALVE, Map.of("duration_min", 30), "AUTO:rule_1");

        assertNotNull(result);
        assertNotNull(result.getCmdId());
        assertTrue(result.getCmdId().startsWith("cmd_"));
        assertEquals("valve_001", result.getDeviceId());
        assertEquals(CommandLog.CommandAction.OPEN_VALVE, result.getAction());
        assertEquals(CommandLog.CommandStatus.SENT, result.getStatus());

        verify(commandLogRepository, times(2)).save(any(CommandLog.class));
        verify(mqttPublisher).publish(contains("valve_001"), any());
    }

    @Test
    void handleAck_confirmed_updatesStatus() {
        CommandLog cmd = new CommandLog();
        cmd.setCmdId("cmd_test");
        cmd.setStatus(CommandLog.CommandStatus.SENT);
        when(commandLogRepository.findByCmdId("cmd_test")).thenReturn(Optional.of(cmd));

        commandService.handleAck("cmd_test", "CONFIRMED", null, null, null);

        assertEquals(CommandLog.CommandStatus.CONFIRMED, cmd.getStatus());
        assertNotNull(cmd.getConfirmedAt());
        verify(commandLogRepository).save(cmd);
    }

    @Test
    void handleAck_executed_updatesStatusAndResult() {
        CommandLog cmd = new CommandLog();
        cmd.setCmdId("cmd_test");
        cmd.setStatus(CommandLog.CommandStatus.CONFIRMED);
        when(commandLogRepository.findByCmdId("cmd_test")).thenReturn(Optional.of(cmd));

        Map<String, Object> result = Map.of("flow_liters", 150);
        commandService.handleAck("cmd_test", "EXECUTED", result, null, null);

        assertEquals(CommandLog.CommandStatus.EXECUTED, cmd.getStatus());
        assertNotNull(cmd.getExecutedAt());
        assertEquals(result, cmd.getResult());
        verify(commandLogRepository).save(cmd);
    }

    @Test
    void handleAck_failed_updatesStatusAndError() {
        CommandLog cmd = new CommandLog();
        cmd.setCmdId("cmd_test");
        cmd.setStatus(CommandLog.CommandStatus.SENT);
        when(commandLogRepository.findByCmdId("cmd_test")).thenReturn(Optional.of(cmd));

        commandService.handleAck("cmd_test", "FAILED", null, "VALVE_STUCK", "阀门卡住");

        assertEquals(CommandLog.CommandStatus.FAILED, cmd.getStatus());
        assertNotNull(cmd.getFailedAt());
        verify(commandLogRepository).save(cmd);
    }

    @Test
    void emergencyStop_closesActiveValves() {
        ReflectionTestUtils.setField(commandService, "defaultTenantId", "t001");

        CommandLog active = new CommandLog();
        active.setTenantId(1L);
        active.setDeviceId("valve_001");
        active.setAction(CommandLog.CommandAction.OPEN_VALVE);
        when(commandLogRepository.findByStatusIn(any())).thenReturn(List.of(active));
        when(commandLogRepository.save(any(CommandLog.class))).thenAnswer(i -> i.getArgument(0));

        int count = commandService.emergencyStop(1L);

        assertEquals(1, count);
        verify(mqttPublisher).publish(contains("valve_001"), any());
    }

    @Test
    void getCommand_notFound_throwsException() {
        when(commandLogRepository.findByCmdId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> commandService.getCommand("nonexistent"));
    }
}
