package com.example.insurance.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Claim extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_number", unique = true, nullable = false, length = 50)
    private String claimNumber;

    @Column(name = "policy_number", nullable = false, length = 50)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 30)
    private ClaimType claimType;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @CreationTimestamp
    @Column(name = "claim_date", nullable = false)
    private LocalDateTime claimDate;

    @Column(name = "claimed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private ClaimPriority priority = ClaimPriority.NORMAL;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ClaimType {
        ACCIDENT, ILLNESS, PROPERTY_DAMAGE, THEFT, NATURAL_DISASTER, TRAVEL_CANCELATION, DEATH, DISABILITY, OTHER
    }

    public enum ClaimStatus {
        SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, PAID, CLOSED
    }

    public enum ClaimPriority {
        LOW, NORMAL, HIGH, URGENT
    }
}