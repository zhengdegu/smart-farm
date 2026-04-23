package com.smartfarm.ai.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_patrol_log")
public class PatrolLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String patrolType; // TREND_CHECK / IRRIGATION_EVAL / DEVICE_HEALTH / DAILY_SUMMARY

    private String greenhouseNo;

    @Column(columnDefinition = "TEXT")
    private String finding;

    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.INFO;

    @Enumerated(EnumType.STRING)
    private ActionTaken actionTaken = ActionTaken.IGNORED;

    private Long notifiedUserId;

    @Column(columnDefinition = "jsonb")
    private String rawContext;

    @Column(columnDefinition = "jsonb")
    private String rawResponse;

    public enum Severity { INFO, WARNING, CRITICAL }
    public enum ActionTaken { NOTIFIED, AUTO_EXECUTED, IGNORED }
}
