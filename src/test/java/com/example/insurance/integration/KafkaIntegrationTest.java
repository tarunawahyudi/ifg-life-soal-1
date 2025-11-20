package com.example.insurance.integration;

import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import com.example.insurance.repository.ClaimRepository;
import com.example.insurance.repository.ClaimAssessmentRepository;
import com.example.insurance.service.KafkaClaimService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class KafkaIntegrationTest {

    @Inject
    KafkaClaimService kafkaClaimService;

    @Inject
    ClaimRepository claimRepository;

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // No need to clean up database - use unique claim numbers for each test
    }

    @Test
    public void testProcessClaimSubmission_DatabaseIntegration() throws Exception {
        String claimSubmissionJson = """
            {
                "claimNumber": "INT-001",
                "policyNumber": "POL-INT-001",
                "claimType": "ACCIDENT",
                "incidentDate": "2024-01-15",
                "claimedAmount": 5000.00,
                "description": "Integration test accident claim",
                "priority": "NORMAL",
                "policyholderId": "PH-INT-001",
                "policyholderName": "Integration Test User",
                "policyholderEmail": "test@integration.com"
            }
            """;

        // Process the claim directly through the service
        kafkaClaimService.processClaimSubmission(claimSubmissionJson);

        // Verify claim was saved to database
        Claim savedClaim = claimRepository.findByClaimNumber("INT-001")
                .orElseThrow(() -> new AssertionError("Claim should be saved to database"));

        assertEquals("INT-001", savedClaim.getClaimNumber());
        assertEquals("POL-INT-001", savedClaim.getPolicyNumber());
        assertEquals(Claim.ClaimType.ACCIDENT, savedClaim.getClaimType());
        assertEquals(new BigDecimal("5000.00"), savedClaim.getClaimedAmount());
        assertEquals("Integration test accident claim", savedClaim.getDescription());
        assertEquals(Claim.ClaimPriority.NORMAL, savedClaim.getPriority());
        assertEquals(Claim.ClaimStatus.SUBMITTED, savedClaim.getStatus());

        // Verify assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("INT-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved to database"));

        assertEquals("INT-001", savedAssessment.getClaimNumber());
        assertNotNull(savedAssessment.getAssessorId());
        assertTrue(savedAssessment.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(savedAssessment.getApprovedAmount().compareTo(savedClaim.getClaimedAmount()) <= 0);
        assertTrue(savedAssessment.getRiskScore() >= 0 && savedAssessment.getRiskScore() <= 100);
        assertFalse(savedAssessment.getFraudFlag());
        assertNotNull(savedAssessment.getAssessmentNotes());
        assertTrue(savedAssessment.getProcessingTimeMs() > 0);
    }

    @Test
    public void testHighPriorityClaimIntegration() throws Exception {
        String highPriorityClaimJson = """
            {
                "claimNumber": "INT-HIGH-001",
                "policyNumber": "POL-HIGH-001",
                "claimType": "NATURAL_DISASTER",
                "incidentDate": "2024-02-20",
                "claimedAmount": 15000.00,
                "description": "High priority flood damage claim",
                "priority": "URGENT",
                "policyholderId": "PH-HIGH-001",
                "policyholderName": "High Priority User",
                "policyholderEmail": "urgent@integration.com"
            }
            """;

        kafkaClaimService.processClaimSubmission(highPriorityClaimJson);

        // Verify claim was saved with correct priority
        Claim savedClaim = claimRepository.findByClaimNumber("INT-HIGH-001")
                .orElseThrow(() -> new AssertionError("High priority claim should be saved"));

        assertEquals(Claim.ClaimPriority.URGENT, savedClaim.getPriority());
        assertEquals(Claim.ClaimStatus.SUBMITTED, savedClaim.getStatus());

        // Verify assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("INT-HIGH-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for high priority claim"));

        assertNotNull(savedAssessment);
        assertTrue(savedAssessment.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testFraudDetectionIntegration() throws Exception {
        String suspiciousClaimJson = """
            {
                "claimNumber": "INT-FRAUD-001",
                "policyNumber": "POL-FRAUD-001",
                "claimType": "THEFT",
                "incidentDate": "2024-03-10",
                "claimedAmount": 75000.00,
                "description": "Very high value theft claim",
                "priority": "HIGH",
                "policyholderId": "PH-FRAUD-001",
                "policyholderName": "Suspicious User",
                "policyholderEmail": "suspicious@integration.com"
            }
            """;

        kafkaClaimService.processClaimSubmission(suspiciousClaimJson);

        // Verify claim was saved
        Claim savedClaim = claimRepository.findByClaimNumber("INT-FRAUD-001")
                .orElseThrow(() -> new AssertionError("Fraud-flagged claim should be saved"));

        assertEquals(new BigDecimal("75000.00"), savedClaim.getClaimedAmount());
        assertEquals(Claim.ClaimType.THEFT, savedClaim.getClaimType());
        assertEquals(Claim.ClaimPriority.HIGH, savedClaim.getPriority());

        // Verify assessment detects fraud
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("INT-FRAUD-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for fraud claim"));

        assertTrue(savedAssessment.getFraudFlag(), "Assessment should flag as fraud");
        assertTrue(savedAssessment.getRiskScore() > 70, "High risk score expected for fraud claim");
        assertTrue(savedAssessment.getAssessmentNotes().toLowerCase().contains("fraud"));
    }

    @Test
    public void testHighPriorityTopicIntegration() throws Exception {
        String urgentClaimJson = """
            {
                "claimNumber": "INT-URGENT-001",
                "policyNumber": "POL-URGENT-001",
                "claimType": "ILLNESS",
                "incidentDate": "2024-03-15",
                "claimedAmount": 8000.00,
                "description": "Medical emergency claim",
                "priority": "HIGH",
                "policyholderId": "PH-URGENT-001",
                "policyholderName": "Emergency Patient",
                "policyholderEmail": "emergency@integration.com"
            }
            """;

        // Test the high priority topic processing
        kafkaClaimService.processHighPriorityClaim(urgentClaimJson);

        // Verify urgent claim was processed correctly
        Claim savedClaim = claimRepository.findByClaimNumber("INT-URGENT-001")
                .orElseThrow(() -> new AssertionError("Urgent claim should be saved"));

        assertEquals(Claim.ClaimStatus.UNDER_REVIEW, savedClaim.getStatus());
        assertEquals(Claim.ClaimPriority.HIGH, savedClaim.getPriority());

        // Verify express assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("INT-URGENT-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for urgent claim"));

        assertEquals("EXPRESS_ASSESSOR", savedAssessment.getAssessorId());
        assertEquals(200, savedAssessment.getProcessingTimeMs());
        assertFalse(savedAssessment.getFraudFlag());
    }

    @Test
    public void testInvalidJsonHandlingIntegration() throws Exception {
        String invalidJson = """
            {
                "invalid": "json",
                "missing": "required fields"
            }
            """;

        // Should throw exception for invalid JSON
        assertThrows(RuntimeException.class, () -> {
            kafkaClaimService.processClaimSubmission(invalidJson);
        });

        // Verify nothing was saved to database
        assertTrue(claimRepository.findAll().list().isEmpty(),
                "Should not save any claims for invalid JSON");
        assertTrue(assessmentRepository.findAll().list().isEmpty(),
                "Should not save any assessments for invalid JSON");
    }

    @Test
    public void testMultipleClaimsProcessingIntegration() throws Exception {
        String[] claimJsons = {
            """
            {
                "claimNumber": "INT-MULTI-001",
                "policyNumber": "POL-MULTI-001",
                "claimType": "ACCIDENT",
                "incidentDate": "2024-01-10",
                "claimedAmount": 3000.00,
                "description": "First test claim",
                "priority": "NORMAL"
            }
            """,
            """
            {
                "claimNumber": "INT-MULTI-002",
                "policyNumber": "POL-MULTI-002",
                "claimType": "PROPERTY_DAMAGE",
                "incidentDate": "2024-01-11",
                "claimedAmount": 6000.00,
                "description": "Second test claim",
                "priority": "HIGH"
            }
            """,
            """
            {
                "claimNumber": "INT-MULTI-003",
                "policyNumber": "POL-MULTI-003",
                "claimType": "TRAVEL_CANCELATION",
                "incidentDate": "2024-01-12",
                "claimedAmount": 2000.00,
                "description": "Third test claim",
                "priority": "LOW"
            }
            """
        };

        // Process multiple claims
        for (String claimJson : claimJsons) {
            kafkaClaimService.processClaimSubmission(claimJson);
        }

        // Verify all claims were saved
        for (int i = 1; i <= 3; i++) {
            String claimNumber = "INT-MULTI-00" + i;
            assertTrue(claimRepository.findByClaimNumber(claimNumber).isPresent(),
                    "Claim " + claimNumber + " should be saved");

            assertTrue(assessmentRepository.findByClaimNumber(claimNumber).isPresent(),
                    "Assessment for " + claimNumber + " should be saved");
        }

        // Verify final counts
        assertEquals(3, claimRepository.count(), "Should have exactly 3 claims saved");
        assertEquals(3, assessmentRepository.count(), "Should have exactly 3 assessments saved");
    }
}