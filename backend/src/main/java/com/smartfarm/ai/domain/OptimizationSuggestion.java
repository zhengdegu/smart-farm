package com.smartfarm.ai.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_optimization_suggestion")
public class OptimizationSuggestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String greenhouseNo;

    private String suggestionType; // THRESHOLD / DURATION / SCHEDULE

    @Column(columnDefinition = "jsonb")
    private String currentValue;

    @Column(columnDefinition = "jsonb")
    private String suggestedValue;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Enumerated(EnumType.STRING)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    private Long acceptedBy;

    public enum SuggestionStatus { PENDING, ACCEPTED, REJECTED }
}
