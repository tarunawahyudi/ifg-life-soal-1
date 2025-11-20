package com.example.insurance.service;

import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import com.example.insurance.util.SampleDataGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.LocalDateTime;

@ApplicationScoped
public class KafkaProducerService {

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

    @Inject
    @Channel("claim-submissions")
    Emitter<String> claimSubmissionsEmitter;

    @Inject
    @Channel("high-priority-claims")
    Emitter<String> highPriorityClaimsEmitter;

    public void sendProcessedClaimEvent(Claim claim, ClaimAssessment assessment) {
        try {
            String event = buildProcessedClaimEvent(claim, assessment);

            Log.info("[KAFKA-PRODUCER] Sending processed claim event to processed-claims topic for: " + claim.getClaimNumber());
            Log.debug("[KAFKA-PRODUCER] Event data: " + event);

            processedClaimsEmitter.send(event)
                    .whenComplete((success, failure) -> {
                        if (failure == null) {
                            Log.info("[KAFKA-PRODUCER] Successfully sent processed claim event for: " + claim.getClaimNumber());
                        } else {
                            Log.error("[KAFKA-PRODUCER] Failed to send processed claim event for: " + claim.getClaimNumber(), failure);
                        }
                    });
        } catch (Exception e) {
            Log.error("[KAFKA-PRODUCER] Error creating processed claim event: " + e.getMessage(), e);
        }
    }

    public void sendFraudAlert(Claim claim, ClaimAssessment assessment) {
        try {
            String alert = buildFraudAlert(claim, assessment);

            Log.warn("[KAFKA-PRODUCER] Sending FRAUD ALERT to fraud-alerts topic for claim: " + claim.getClaimNumber() +
                    " (Risk Score: " + assessment.getRiskScore() + ")");
            Log.debug("[KAFKA-PRODUCER] Fraud alert data: " + alert);

            fraudAlertsEmitter.send(alert)
                    .whenComplete((success, failure) -> {
                        if (failure == null) {
                            Log.warn("[KAFKA-PRODUCER] Successfully sent fraud alert for claim: " + claim.getClaimNumber());
                        } else {
                            Log.error("[KAFKA-PRODUCER] Failed to send fraud alert for claim: " + claim.getClaimNumber(), failure);
                        }
                    });
        } catch (Exception e) {
            Log.error("[KAFKA-PRODUCER] Error creating fraud alert: " + e.getMessage(), e);
        }
    }

    public void sendHighPriorityNotification(Claim claim, ClaimAssessment assessment) {
        try {
            String notification = buildHighPriorityNotification(claim, assessment);

            claimEventsEmitter.send(notification);
            Log.info("Sent high priority notification for claim: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending high priority notification: " + e.getMessage(), e);
        }
    }

    public void sendClaimLifecycleEvent(Claim claim, String eventType) {
        try {
            String event = buildClaimLifecycleEvent(claim, eventType);

            claimEventsEmitter.send(event);
            Log.info("Sent lifecycle event for claim: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending claim lifecycle event: " + e.getMessage(), e);
        }
    }

    public void sendUrgentProcessedClaimEvent(Claim claim, ClaimAssessment assessment) {
        try {
            String event = buildUrgentProcessedClaimEvent(claim, assessment);

            processedClaimsEmitter.send(event);
            Log.info("Sent urgent processed claim event for: " + claim.getClaimNumber());
        } catch (Exception e) {
            Log.error("Error sending urgent processed claim event: " + e.getMessage(), e);
        }
    }

    public void publishClaimSubmission(ClaimSubmission claimSubmission) {
        try {
            String claimJson = objectMapper.writeValueAsString(claimSubmission);

            Log.info("[KAFKA-PRODUCER] Publishing claim to claim-submissions topic: " + claimSubmission.getClaimNumber());
            Log.debug("[KAFKA-PRODUCER] Claim data: " + claimJson);

            claimSubmissionsEmitter.send(claimJson)
                    .whenComplete((success, failure) -> {
                        if (failure == null) {
                            Log.info("[KAFKA-PRODUCER] Successfully published claim to Kafka: " + claimSubmission.getClaimNumber());
                        } else {
                            Log.error("[KAFKA-PRODUCER] Failed to publish claim to Kafka: " + claimSubmission.getClaimNumber(), failure);
                            throw new RuntimeException("Failed to publish claim to Kafka", failure);
                        }
                    });
        } catch (Exception e) {
            Log.error("[KAFKA-PRODUCER] Error publishing claim to Kafka: " + e.getMessage(), e);
            throw new RuntimeException("Failed to publish claim to Kafka", e);
        }
    }

    public void publishHighPriorityClaim(ClaimSubmission claimSubmission) {
        try {
            String claimJson = objectMapper.writeValueAsString(claimSubmission);

            Log.info("[KAFKA-PRODUCER] Publishing high priority claim to high-priority-claims topic: " + claimSubmission.getClaimNumber());
            Log.debug("[KAFKA-PRODUCER] High priority claim data: " + claimJson);

            highPriorityClaimsEmitter.send(claimJson)
                    .whenComplete((success, failure) -> {
                        if (failure == null) {
                            Log.info("[KAFKA-PRODUCER] Successfully published high priority claim to Kafka: " + claimSubmission.getClaimNumber());
                        } else {
                            Log.error("[KAFKA-PRODUCER] Failed to publish high priority claim to Kafka: " + claimSubmission.getClaimNumber(), failure);
                            throw new RuntimeException("Failed to publish high priority claim to Kafka", failure);
                        }
                    });
        } catch (Exception e) {
            Log.error("[KAFKA-PRODUCER] Error publishing high priority claim to Kafka: " + e.getMessage(), e);
            throw new RuntimeException("Failed to publish high priority claim to Kafka", e);
        }
    }

    private String buildProcessedClaimEvent(Claim claim, ClaimAssessment assessment) {
        return String.format("""
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
    }

    private String buildFraudAlert(Claim claim, ClaimAssessment assessment) {
        return String.format("""
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
    }

    private String buildHighPriorityNotification(Claim claim, ClaimAssessment assessment) {
        return String.format("""
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
    }

    private String buildClaimLifecycleEvent(Claim claim, String eventType) {
        return String.format("""
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
    }

    private String buildUrgentProcessedClaimEvent(Claim claim, ClaimAssessment assessment) {
        return String.format("""
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
    }
}