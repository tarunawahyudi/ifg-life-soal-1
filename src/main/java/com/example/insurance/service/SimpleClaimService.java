package com.example.insurance.service;

import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import com.example.insurance.repository.ClaimRepository;
import com.example.insurance.repository.ClaimAssessmentRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SimpleClaimService {

    @Inject
    ClaimRepository claimRepository;

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    @Transactional
    public Claim submitClaim(ClaimSubmission claimSubmission) {
        Log.info("Processing claim submission: " + claimSubmission.getClaimNumber());

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

        // Create basic assessment
        ClaimAssessment assessment = ClaimAssessment.builder()
                .claimNumber(claim.getClaimNumber())
                .assessorId("SIMPLE_ASSESSOR")
                .approvedAmount(claim.getClaimedAmount().multiply(BigDecimal.valueOf(0.8)))
                .riskScore(25)
                .fraudFlag(false)
                .assessmentNotes("Basic assessment - approved 80%")
                .processingTimeMs(500)
                .build();

        assessmentRepository.create(assessment);

        Log.info("Claim submitted successfully: " + claim.getClaimNumber());
        return claim;
    }

    public Optional<Claim> getClaimByNumber(String claimNumber) {
        return claimRepository.findByClaimNumber(claimNumber);
    }

    public List<Claim> getAllClaims() {
        return claimRepository.listAll();
    }

    public List<Claim> getPendingClaims() {
        return claimRepository.findPendingClaims();
    }

    @Transactional
    public Claim updateClaimStatus(String claimNumber, Claim.ClaimStatus newStatus) {
        Optional<Claim> claimOpt = claimRepository.findByClaimNumber(claimNumber);
        if (claimOpt.isPresent()) {
            Claim claim = claimOpt.get();
            claim.setStatus(newStatus);
            return claimRepository.createOrUpdate(claim);
        }
        return null;
    }
}