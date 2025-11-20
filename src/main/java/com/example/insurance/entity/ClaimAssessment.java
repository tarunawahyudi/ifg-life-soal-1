package com.example.insurance.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAssessment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_number", nullable = false, length = 50)
    private String claimNumber;

    @Column(name = "assessor_id", length = 50)
    private String assessorId;

    @CreationTimestamp
    @Column(name = "assessment_date", nullable = false)
    private LocalDateTime assessmentDate;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "fraud_flag", nullable = false)
    @Builder.Default
    private Boolean fraudFlag = false;

    @Column(name = "assessment_notes", columnDefinition = "TEXT")
    private String assessmentNotes;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}