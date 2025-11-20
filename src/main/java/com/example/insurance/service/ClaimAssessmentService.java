package com.example.insurance.service;

import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class ClaimAssessmentService {

    private final Random random = new Random();

    
    public ClaimAssessment performClaimAssessment(Claim claim) {
        Log.debug("[ASSESSMENT] Starting standard claim assessment for: " + claim.getClaimNumber());

        ClaimAssessment assessment = ClaimAssessment.builder()
                .claimNumber(claim.getClaimNumber())
                .assessorId("KAFKA_ASSESSOR_" + UUID.randomUUID().toString().substring(0, 6))
                .approvedAmount(calculateApprovedAmount(claim))
                .riskScore(calculateRiskScore(claim))
                .fraudFlag(detectFraud(claim))
                .assessmentNotes(generateAssessmentNotes(claim))
                .processingTimeMs((int) calculateProcessingTime())
                .build();

        Log.debug("[ASSESSMENT] Standard assessment completed for: " + claim.getClaimNumber() +
                " | Approved: " + assessment.getApprovedAmount() +
                " | Risk: " + assessment.getRiskScore() +
                " | Fraud: " + assessment.getFraudFlag());

        return assessment;
    }

    public ClaimAssessment performExpressAssessment(Claim claim) {
        Log.debug("[ASSESSMENT] Starting EXPRESS assessment for high priority claim: " + claim.getClaimNumber());

        ClaimAssessment assessment = ClaimAssessment.builder()
                .claimNumber(claim.getClaimNumber())
                .assessorId("EXPRESS_ASSESSOR")
                .approvedAmount(claim.getClaimedAmount().multiply(BigDecimal.valueOf(0.9)))
                .riskScore(15)
                .fraudFlag(false)
                .assessmentNotes("Express assessment for high priority claim")
                .processingTimeMs(200)
                .build();

        Log.debug("[ASSESSMENT] Express assessment completed for: " + claim.getClaimNumber() +
                " | Approved: " + assessment.getApprovedAmount() +
                " | Processing Time: " + assessment.getProcessingTimeMs() + "ms");

        return assessment;
    }

    private BigDecimal calculateApprovedAmount(Claim claim) {
        BigDecimal baseAmount = claim.getClaimedAmount();
        BigDecimal multiplier = switch (claim.getClaimType()) {
            case ACCIDENT -> BigDecimal.valueOf(0.85);
            case ILLNESS, DISABILITY -> BigDecimal.valueOf(0.9);
            case PROPERTY_DAMAGE -> BigDecimal.valueOf(0.8);
            case THEFT -> BigDecimal.valueOf(0.75);
            case NATURAL_DISASTER -> BigDecimal.valueOf(0.95);
            case TRAVEL_CANCELATION -> BigDecimal.valueOf(0.7);
            case DEATH -> BigDecimal.valueOf(1.0);
            case OTHER -> BigDecimal.valueOf(0.6);
        };

        BigDecimal approvedAmount = baseAmount.multiply(multiplier);
        Log.debug("[ASSESSMENT] Calculated approved amount for " + claim.getClaimType() +
                " claim: " + approvedAmount + " (from " + baseAmount + ")");

        return approvedAmount;
    }

    private int calculateRiskScore(Claim claim) {
        int baseScore = random.nextInt(50) + 10;

        // Higher risk for large amounts
        if (claim.getClaimedAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            baseScore += 20;
            Log.debug("[ASSESSMENT] Added 20 risk points for high claim amount: " + claim.getClaimedAmount());
        }

        // Higher risk for certain claim types
        if (claim.getClaimType() == Claim.ClaimType.THEFT ||
            claim.getClaimType() == Claim.ClaimType.NATURAL_DISASTER) {
            baseScore += 15;
            Log.debug("[ASSESSMENT] Added 15 risk points for claim type: " + claim.getClaimType());
        }

        int finalScore = Math.min(baseScore, 100);
        Log.debug("[ASSESSMENT] Final risk score for " + claim.getClaimNumber() + ": " + finalScore);

        return finalScore;
    }

    private boolean detectFraud(Claim claim) {
        int fraudProbability = calculateRiskScore(claim);
        boolean isFraud = fraudProbability > 70 || claim.getClaimedAmount().compareTo(BigDecimal.valueOf(50000)) > 0;

        if (isFraud) {
            Log.warn("[FRAUD-DETECTION] Fraud indicators detected for claim: " + claim.getClaimNumber() +
                    " | Risk Score: " + fraudProbability +
                    " | Amount: " + claim.getClaimedAmount());
        }

        return isFraud;
    }

    private String generateAssessmentNotes(Claim claim) {
        String notes = String.format("Standard assessment for %s claim. Risk score: %d. %s",
                claim.getClaimType(),
                calculateRiskScore(claim),
                detectFraud(claim) ? "Flagged for potential fraud." : "No fraud indicators detected.");

        Log.debug("[ASSESSMENT] Generated assessment notes for " + claim.getClaimNumber() + ": " + notes);
        return notes;
    }

    private long calculateProcessingTime() {
        long processingTime = 300 + random.nextInt(700);
        Log.debug("[ASSESSMENT] Simulated processing time: " + processingTime + "ms");
        return processingTime;
    }
}