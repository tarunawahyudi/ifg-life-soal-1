package com.example.insurance.service;

import com.example.insurance.dto.ClaimSubmission;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class KafkaClaimConsumerService {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ClaimProcessorService claimProcessor;

    @Incoming("claim-submissions")
    @RunOnVirtualThread
    public void processClaimSubmission(String claimJson) {
        String startTime = java.time.Instant.now().toString();

        try {
            Log.info("[KAFKA-CONSUMER] Starting to process claim submission from claim-submissions topic");
            Log.info("[KAFKA-CONSUMER] Processing started at: " + startTime);
            Log.debug("[KAFKA-CONSUMER] Claim JSON data: " + claimJson);

            ClaimSubmission claimSubmission = objectMapper.readValue(claimJson, ClaimSubmission.class);
            Log.info("[KAFKA-CONSUMER] Successfully parsed claim submission for policy: " + claimSubmission.getPolicyNumber());

            claimProcessor.processClaimSubmission(claimSubmission);

            Log.info("[KAFKA-CONSUMER] Claim submission processing completed successfully");

        } catch (Exception e) {
            Log.error("[KAFKA-CONSUMER] Error processing claim submission: " + e.getMessage(), e);
            Log.error("[KAFKA-CONSUMER] Failed claim data: " + claimJson);
            throw new RuntimeException("Failed to process claim submission", e);
        }
    }

    @Incoming("high-priority-claims")
    @RunOnVirtualThread
    public void processHighPriorityClaim(String claimJson) {
        String startTime = java.time.Instant.now().toString();

        try {
            Log.info("[KAFKA-CONSUMER] Starting to process HIGH PRIORITY claim from high-priority-claims topic");
            Log.info("[KAFKA-CONSUMER] High priority processing started at: " + startTime);
            Log.debug("[KAFKA-CONSUMER] High priority claim JSON data: " + claimJson);

            ClaimSubmission claimSubmission = objectMapper.readValue(claimJson, ClaimSubmission.class);
            Log.info("[KAFKA-CONSUMER] Successfully parsed high priority claim submission for policy: " + claimSubmission.getPolicyNumber());

            claimProcessor.processHighPriorityClaim(claimSubmission);

            Log.info("[KAFKA-CONSUMER] High priority claim processing completed successfully");

        } catch (Exception e) {
            Log.error("[KAFKA-CONSUMER] Error processing high priority claim: " + e.getMessage(), e);
            Log.error("[KAFKA-CONSUMER] Failed high priority claim data: " + claimJson);
            throw new RuntimeException("Failed to process high priority claim", e);
        }
    }
}