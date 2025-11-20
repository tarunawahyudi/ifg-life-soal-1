package com.example.insurance.service;

import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import com.example.insurance.repository.ClaimRepository;
import com.example.insurance.repository.ClaimAssessmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class KafkaClaimService {

    @Inject
    ClaimRepository claimRepository;

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Channel("processed-claims")
    Emitter<String> processedClaimsEmitter;

    @Inject
    @Channel("fraud-alerts")
    Emitter<String> fraudAlertsEmitter;

    @Inject
    @Channel("claim-events")
    Emitter<String> claimEventsEmitter;

    private final Random random = new Random();

    @Incoming("claim-submissions")
    @Transactional
    public void processClaimSubmission(String claimJson) {
        try {
            Log.info("Received claim submission from Kafka: " + claimJson);

            ClaimSubmission claimSubmission = objectMapper.readValue(claimJson, ClaimSubmission.class);

            Claim claim = Claim.builder()
                    .claimNumber(claimSubmission.getClaimNumber() != null ?
                        claimSubmission.getClaimNumber() :
                        "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .policyNumber(claimSubmission.getPolicyNumber())
                    .claimType(claimSubmission.getClaimType())
                    .incidentDate(claimSubmission.getIncidentDate())
                    .claimedAmount(claimSubmission.getClaimedAmount())
                    .description(claimSubmission.getDescription())
                    .priority(claimSubmission.getPriority())
                    .status(Claim.ClaimStatus.SUBMITTED)
                    .build();

            claimRepository.createOrUpdate(claim);
            Log.info("Claim saved to database: " + claim.getClaimNumber());

            ClaimAssessment assessment = performClaimAssessment(claim);
            assessmentRepository.create(assessment);
            Log.info("Claim assessment created: " + assessment.getClaimNumber());

            if (assessment.getFraudFlag()) {
                sendFraudAlert(claim, assessment);
            }

            if (claim.getPriority() == Claim.ClaimPriority.HIGH || claim.getPriority() == Claim.ClaimPriority.URGENT) {
                sendHighPriorityNotification(claim, assessment);
            }

            sendProcessedClaimEvent(claim, assessment);
            sendClaimLifecycleEvent(claim, "CLAIM_PROCESSED");

        } catch (Exception e) {
            Log.error("Error processing claim submission: " + e.getMessage(), e);
            throw new RuntimeException("Failed to process claim submission", e);
        }
    }

    @Incoming("high-priority-claims")
    @Transactional
    public void processHighPriorityClaim(String claimJson) {
        try {
            Log.info("Processing high priority claim: " + claimJson);

            ClaimSubmission claimSubmission = objectMapper.readValue(claimJson, ClaimSubmission.class);

            Claim claim = Claim.builder()
                    .claimNumber(claimSubmission.getClaimNumber() != null ?
                        claimSubmission.getClaimNumber() :
                        "HP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .policyNumber(claimSubmission.getPolicyNumber())
                    .claimType(claimSubmission.getClaimType())
                    .incidentDate(claimSubmission.getIncidentDate())
                    .claimedAmount(claimSubmission.getClaimedAmount())
                    .description(claimSubmission.getDescription())
                    .priority(Claim.ClaimPriority.HIGH)
                    .status(Claim.ClaimStatus.UNDER_REVIEW)
                    .build();

            claimRepository.createOrUpdate(claim);

            ClaimAssessment assessment = performExpressAssessment(claim);
            assessmentRepository.create(assessment);

            sendUrgentProcessedClaimEvent(claim, assessment);
            sendClaimLifecycleEvent(claim, "HIGH_PRIORITY_CLAIM_PROCESSED");

        } catch (Exception e) {
            Log.error("Error processing high priority claim: " + e.getMessage(), e);
            throw new RuntimeException("Failed to process high priority claim", e);
        }
    }

    private ClaimAssessment performClaimAssessment(Claim claim) {
        return ClaimAssessment.builder()
                .claimNumber(claim.getClaimNumber())
                .assessorId("KAFKA_ASSESSOR_" + UUID.randomUUID().toString().substring(0, 6))
                .approvedAmount(calculateApprovedAmount(claim))
                .riskScore(calculateRiskScore(claim))
                .fraudFlag(detectFraud(claim))
                .assessmentNotes(generateAssessmentNotes(claim))
                .processingTimeMs((int) calculateProcessingTime())
                .build();
    }

    private ClaimAssessment performExpressAssessment(Claim claim) {
        return ClaimAssessment.builder()
                .claimNumber(claim.getClaimNumber())
                .assessorId("EXPRESS_ASSESSOR")
                .approvedAmount(claim.getClaimedAmount().multiply(BigDecimal.valueOf(0.9)))
                .riskScore(15)
                .fraudFlag(false)
                .assessmentNotes("Express assessment for high priority claim")
                .processingTimeMs(200)
                .build();
    }

    private BigDecimal calculateApprovedAmount(Claim claim) {
        BigDecimal baseAmount = claim.getClaimedAmount();
        BigDecimal multiplier = switch (claim.getClaimType()) {
            case ACCIDENT -> BigDecimal.valueOf(0.85);
            case ILLNESS -> BigDecimal.valueOf(0.9);
            case PROPERTY_DAMAGE -> BigDecimal.valueOf(0.8);
            case THEFT -> BigDecimal.valueOf(0.75);
            case NATURAL_DISASTER -> BigDecimal.valueOf(0.95);
            case TRAVEL_CANCELATION -> BigDecimal.valueOf(0.7);
            case DEATH -> BigDecimal.valueOf(1.0);
            case DISABILITY -> BigDecimal.valueOf(0.9);
            case OTHER -> BigDecimal.valueOf(0.6);
        };

        return baseAmount.multiply(multiplier);
    }

    private int calculateRiskScore(Claim claim) {
        int baseScore = random.nextInt(50) + 10;

        if (claim.getClaimedAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            baseScore += 20;
        }

        if (claim.getClaimType() == Claim.ClaimType.THEFT || claim.getClaimType() == Claim.ClaimType.NATURAL_DISASTER) {
            baseScore += 15;
        }

        return Math.min(baseScore, 100);
    }

    private boolean detectFraud(Claim claim) {
        int fraudProbability = calculateRiskScore(claim);
        return fraudProbability > 70 || claim.getClaimedAmount().compareTo(BigDecimal.valueOf(50000)) > 0;
    }

    private String generateAssessmentNotes(Claim claim) {
        return String.format("Standard assessment for %s claim. Risk score: %d. %s",
                claim.getClaimType(),
                calculateRiskScore(claim),
                detectFraud(claim) ? "Flagged for potential fraud." : "No fraud indicators detected.");
    }

    private long calculateProcessingTime() {
        return 300 + random.nextInt(700);
    }

    private void sendProcessedClaimEvent(Claim claim, ClaimAssessment assessment) {
        try {
            String event = String.format("""
                {
                    "eventType": "CLAIM_PROCESSED",
                    "claimNumber": "%s",
                    "policyNumber": "%s",
                    "claimType": "%s",
                    "claimedAmount": %s,
                    "approvedAmount": %s,
                    "riskScore": %d,
                    "fraudFlag": %s,
                    "processingTimeMs": %d,
                    "timestamp": "%s"
                }
                """,
                claim.getClaimNumber(),
                claim.getPolicyNumber(),
                claim.getClaimType(),
                claim.getClaimedAmount(),
                assessment.getApprovedAmount(),
                assessment.getRiskScore(),
                assessment.getFraudFlag(),
                assessment.getProcessingTimeMs(),
                LocalDateTime.now()
            );

            processedClaimsEmitter.send(event);
            Log.info("Sent processed claim event for: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending processed claim event: " + e.getMessage(), e);
        }
    }

    private void sendUrgentProcessedClaimEvent(Claim claim, ClaimAssessment assessment) {
        try {
            String event = String.format("""
                {
                    "eventType": "URGENT_CLAIM_PROCESSED",
                    "claimNumber": "%s",
                    "policyNumber": "%s",
                    "priority": "%s",
                    "approvedAmount": %s,
                    "processingTimeMs": %d,
                    "timestamp": "%s"
                }
                """,
                claim.getClaimNumber(),
                claim.getPolicyNumber(),
                claim.getPriority(),
                assessment.getApprovedAmount(),
                assessment.getProcessingTimeMs(),
                LocalDateTime.now()
            );

            processedClaimsEmitter.send(event);
            Log.info("Sent urgent processed claim event for: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending urgent processed claim event: " + e.getMessage(), e);
        }
    }

    private void sendFraudAlert(Claim claim, ClaimAssessment assessment) {
        try {
            String alert = String.format("""
                {
                    "alertType": "FRAUD_DETECTED",
                    "claimNumber": "%s",
                    "policyNumber": "%s",
                    "claimedAmount": %s,
                    "riskScore": %d,
                    "assessorId": "%s",
                    "assessmentNotes": "%s",
                    "timestamp": "%s"
                }
                """,
                claim.getClaimNumber(),
                claim.getPolicyNumber(),
                claim.getClaimedAmount(),
                assessment.getRiskScore(),
                assessment.getAssessorId(),
                assessment.getAssessmentNotes(),
                LocalDateTime.now()
            );

            fraudAlertsEmitter.send(alert);
            Log.warn("Sent fraud alert for claim: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending fraud alert: " + e.getMessage(), e);
        }
    }

    private void sendHighPriorityNotification(Claim claim, ClaimAssessment assessment) {
        try {
            String notification = String.format("""
                {
                    "notificationType": "HIGH_PRIORITY_CLAIM",
                    "claimNumber": "%s",
                    "policyNumber": "%s",
                    "claimType": "%s",
                    "priority": "%s",
                    "claimedAmount": %s,
                    "timestamp": "%s"
                }
                """,
                claim.getClaimNumber(),
                claim.getPolicyNumber(),
                claim.getClaimType(),
                claim.getPriority(),
                claim.getClaimedAmount(),
                LocalDateTime.now()
            );

            claimEventsEmitter.send(notification);
            Log.info("Sent high priority notification for claim: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending high priority notification: " + e.getMessage(), e);
        }
    }

    private void sendClaimLifecycleEvent(Claim claim, String eventType) {
        try {
            String event = String.format("""
                {
                    "eventType": "%s",
                    "claimNumber": "%s",
                    "policyNumber": "%s",
                    "status": "%s",
                    "priority": "%s",
                    "claimType": "%s",
                    "timestamp": "%s"
                }
                """,
                eventType,
                claim.getClaimNumber(),
                claim.getPolicyNumber(),
                claim.getStatus(),
                claim.getPriority(),
                claim.getClaimType(),
                LocalDateTime.now()
            );

            claimEventsEmitter.send(event);
            Log.info("Sent lifecycle event for claim: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending claim lifecycle event: " + e.getMessage(), e);
        }
    }
}