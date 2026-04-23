package com.smartfarm.irrigation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {

    Optional<CommandLog> findByCmdId(String cmdId);

    List<CommandLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<CommandLog> findByDeviceIdAndStatusIn(String deviceId, List<CommandLog.CommandStatus> statuses);

    List<CommandLog> findByStatusIn(List<CommandLog.CommandStatus> statuses);

    List<CommandLog> findByStatusInAndCreatedAtBefore(List<CommandLog.CommandStatus> statuses, java.time.OffsetDateTime before);

    List<CommandLog> findByStatusAndSentAtBefore(CommandLog.CommandStatus status, java.time.OffsetDateTime before);
}
