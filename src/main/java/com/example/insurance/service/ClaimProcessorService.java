package com.example.insurance.service;

import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import com.example.insurance.repository.ClaimAssessmentRepository;
import com.example.insurance.repository.ClaimRepository;
import com.example.insurance.repository.InsurancePolicyRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class ClaimProcessorService {

    @Inject
    ClaimRepository claimRepository;

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    @Inject
    InsurancePolicyRepository policyRepository;

    @Inject
    ClaimAssessmentService assessmentService;

    @Inject
    KafkaProducerService producerService;

    @Transactional
    public void processClaimSubmission(ClaimSubmission claimSubmission) {
        String startTime = java.time.Instant.now().toString();

        Log.info("[CLAIM-PROCESSOR] Starting claim processing for policy: " + claimSubmission.getPolicyNumber());

        // Validate that policy exists before processing
        if (!policyRepository.existsByPolicyNumber(claimSubmission.getPolicyNumber())) {
            Log.warn("[VALIDATION] Policy not found: " + claimSubmission.getPolicyNumber() + " - rejecting claim");
            throw new RuntimeException("Policy not found: " + claimSubmission.getPolicyNumber());
        }

        // Create claim entity
        Claim claim = buildClaimFromSubmission(claimSubmission);
        claimRepository.createOrUpdate(claim);
        Log.info("[DATABASE] Claim saved to database: " + claim.getClaimNumber());

        // Perform assessment
        ClaimAssessment assessment = assessmentService.performClaimAssessment(claim);
        assessmentRepository.create(assessment);
        Log.info("[DATABASE] Claim assessment created: " + assessment.getClaimNumber() +
                " | Approved Amount: " + assessment.getApprovedAmount() +
                " | Risk Score: " + assessment.getRiskScore() +
                " | Fraud Flag: " + assessment.getFraudFlag());

        // Handle fraud detection
        handleFraudDetection(claim, assessment);

        // Handle high priority claims
        handleHighPriorityClaims(claim, assessment);

        // Send events
        producerService.sendProcessedClaimEvent(claim, assessment);
        producerService.sendClaimLifecycleEvent(claim, "CLAIM_PROCESSED");

        String endTime = java.time.Instant.now().toString();
        Log.info("[CLAIM-PROCESSOR] Claim processing completed successfully for: " + claim.getClaimNumber());
        Log.info("[CLAIM-PROCESSOR] Processing started at: " + startTime + " | completed at: " + endTime);

    }

    @Transactional
    public void processHighPriorityClaim(ClaimSubmission claimSubmission) {
        String startTime = java.time.Instant.now().toString();

        Log.info("[CLAIM-PROCESSOR] Starting HIGH PRIORITY claim processing for policy: " + claimSubmission.getPolicyNumber());

        // Validate that policy exists before processing
        if (!policyRepository.existsByPolicyNumber(claimSubmission.getPolicyNumber())) {
            Log.warn("[VALIDATION] Policy not found for high priority claim: " + claimSubmission.getPolicyNumber() + " - rejecting claim");
            throw new RuntimeException("Policy not found: " + claimSubmission.getPolicyNumber());
        }

        // Create high priority claim entity
        Claim claim = buildHighPriorityClaimFromSubmission(claimSubmission);
        claimRepository.createOrUpdate(claim);
        Log.info("[DATABASE] High priority claim saved to database: " + claim.getClaimNumber());

        // Perform express assessment
        ClaimAssessment assessment = assessmentService.performExpressAssessment(claim);
        assessmentRepository.create(assessment);
        Log.info("[DATABASE] Express assessment created for high priority claim: " + assessment.getClaimNumber() +
                " | Approved Amount: " + assessment.getApprovedAmount() +
                " | Processing Time: " + assessment.getProcessingTimeMs() + "ms");

        // Send urgent events
        producerService.sendUrgentProcessedClaimEvent(claim, assessment);
        producerService.sendClaimLifecycleEvent(claim, "HIGH_PRIORITY_CLAIM_PROCESSED");

        String endTime = java.time.Instant.now().toString();
        Log.info("[CLAIM-PROCESSOR] High priority claim processing completed successfully for: " + claim.getClaimNumber());
        Log.info("[CLAIM-PROCESSOR] High priority processing started at: " + startTime + " | completed at: " + endTime);

    }

    private void handleFraudDetection(Claim claim, ClaimAssessment assessment) {
        if (assessment.getFraudFlag()) {
            Log.info("[FRAUD-DETECTION] High fraud risk detected for claim: " + claim.getClaimNumber());
            producerService.sendFraudAlert(claim, assessment);
        } else {
            Log.info("[FRAUD-DETECTION] No fraud indicators detected for claim: " + claim.getClaimNumber());
        }
    }

    private void handleHighPriorityClaims(Claim claim, ClaimAssessment assessment) {
        if (claim.getPriority() == Claim.ClaimPriority.HIGH || claim.getPriority() == Claim.ClaimPriority.URGENT) {
            Log.info("[PRIORITY] High/Urgent priority claim detected: " + claim.getClaimNumber() + " (" + claim.getPriority() + ")");
            producerService.sendHighPriorityNotification(claim, assessment);
        }
    }

    private Claim buildClaimFromSubmission(ClaimSubmission claimSubmission) {
        return Claim.builder()
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
    }

    private Claim buildHighPriorityClaimFromSubmission(ClaimSubmission claimSubmission) {
        return Claim.builder()
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
    }
}