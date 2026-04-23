package com.smartfarm.ai.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_conversation")
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String role;  // user / assistant / system

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "jsonb")
    private String functionCall;

    @Column(columnDefinition = "jsonb")
    private String functionResult;

    private String model;
    private Integer tokensUsed;
    private Integer latencyMs;
}
