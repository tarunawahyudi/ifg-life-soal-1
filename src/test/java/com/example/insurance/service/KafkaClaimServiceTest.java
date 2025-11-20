package com.example.insurance.service;

import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import com.example.insurance.repository.ClaimRepository;
import com.example.insurance.repository.ClaimAssessmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class KafkaClaimServiceTest {

    @Inject
    KafkaClaimConsumerService kafkaConsumerService;

    @Inject
    ClaimProcessorService claimProcessorService;

    @Inject
    ClaimAssessmentService assessmentService;

    @Inject
    ClaimRepository claimRepository;

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    @Inject
    ObjectMapper objectMapper;

    private ClaimSubmission sampleClaimSubmission;

    @BeforeEach
    void setUp() {
        // No need to clean up database - use unique claim numbers for each test

        // Create test data using constructors
        sampleClaimSubmission = new ClaimSubmission();
        sampleClaimSubmission.setClaimNumber("CLM-001");
        sampleClaimSubmission.setPolicyNumber("POL-12345");
        sampleClaimSubmission.setClaimType(Claim.ClaimType.ACCIDENT);
        sampleClaimSubmission.setIncidentDate(LocalDate.of(2024, 1, 15));
        sampleClaimSubmission.setClaimedAmount(new BigDecimal("5000.00"));
        sampleClaimSubmission.setDescription("Car accident claim");
        sampleClaimSubmission.setPriority(Claim.ClaimPriority.NORMAL);
        sampleClaimSubmission.setPolicyholderId("PH-001");
        sampleClaimSubmission.setPolicyholderName("John Doe");
        sampleClaimSubmission.setPolicyholderEmail("john.doe@email.com");

        Claim sampleClaim = Claim.builder()
            .claimNumber("CLM-001")
            .policyNumber("POL-12345")
            .claimType(Claim.ClaimType.ACCIDENT)
            .incidentDate(LocalDate.of(2024, 1, 15))
            .claimedAmount(new BigDecimal("5000.00"))
            .description("Car accident claim")
            .priority(Claim.ClaimPriority.NORMAL)
            .status(Claim.ClaimStatus.SUBMITTED)
            .build();

        ClaimAssessment sampleAssessment = ClaimAssessment.builder()
            .claimNumber("CLM-001")
            .assessorId("TEST_ASSESSOR")
            .approvedAmount(new BigDecimal("4250.00"))
            .riskScore(25)
            .fraudFlag(false)
            .assessmentNotes("Test assessment")
            .processingTimeMs(500)
            .build();
    }

    @Test
    public void testProcessClaimSubmission_Success() throws Exception {
        String claimJson = """
            {
                "claimNumber": "CLM-001",
                "policyNumber": "POL-12345",
                "claimType": "ACCIDENT",
                "incidentDate": "2024-01-15",
                "claimedAmount": 5000.00,
                "description": "Car accident claim",
                "priority": "NORMAL",
                "policyholderId": "PH-001",
                "policyholderName": "John Doe",
                "policyholderEmail": "john.doe@email.com"
            }
            """;

        // Process the claim
        kafkaConsumerService.processClaimSubmission(claimJson);

        // Verify claim was saved to database
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-001")
                .orElseThrow(() -> new AssertionError("Claim should be saved to database"));

        assertNotNull(savedClaim);
        assertEquals("CLM-001", savedClaim.getClaimNumber());
        assertEquals("POL-12345", savedClaim.getPolicyNumber());
        assertEquals(Claim.ClaimType.ACCIDENT, savedClaim.getClaimType());
        assertEquals(new BigDecimal("5000.00"), savedClaim.getClaimedAmount());
        assertEquals("Car accident claim", savedClaim.getDescription());
        assertEquals(Claim.ClaimPriority.NORMAL, savedClaim.getPriority());
        assertEquals(Claim.ClaimStatus.SUBMITTED, savedClaim.getStatus());

        // Verify assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved to database"));

        assertNotNull(savedAssessment);
        assertEquals("CLM-001", savedAssessment.getClaimNumber());
        assertNotNull(savedAssessment.getAssessorId());
        assertTrue(savedAssessment.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0);
        assertFalse(savedAssessment.getFraudFlag());
        assertNotNull(savedAssessment.getAssessmentNotes());
        assertTrue(savedAssessment.getProcessingTimeMs() > 0);
    }

    @Test
    public void testProcessClaimSubmission_WithFraudFlag() throws Exception {
        String highRiskClaimJson = """
            {
                "claimNumber": "CLM-002",
                "policyNumber": "POL-67890",
                "claimType": "THEFT",
                "incidentDate": "2024-02-20",
                "claimedAmount": 60000.00,
                "description": "High value theft claim",
                "priority": "HIGH",
                "policyholderId": "PH-002",
                "policyholderName": "Jane Smith",
                "policyholderEmail": "jane@email.com"
            }
            """;

        kafkaConsumerService.processClaimSubmission(highRiskClaimJson);

        // Verify claim was saved
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-002")
                .orElseThrow(() -> new AssertionError("High risk claim should be saved"));

        assertEquals("CLM-002", savedClaim.getClaimNumber());
        assertEquals(Claim.ClaimType.THEFT, savedClaim.getClaimType());
        assertEquals(new BigDecimal("60000.00"), savedClaim.getClaimedAmount());
        assertEquals(Claim.ClaimPriority.HIGH, savedClaim.getPriority());

        // Verify assessment with fraud detection was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-002")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for high risk claim"));

        assertNotNull(savedAssessment);
        assertTrue(savedAssessment.getFraudFlag(), "High value theft claim should be flagged as fraud");
        assertTrue(savedAssessment.getRiskScore() > 70, "High risk score expected for fraud claim");
        assertTrue(savedAssessment.getAssessmentNotes().toLowerCase().contains("fraud"));
    }

    @Test
    public void testProcessHighPriorityClaim_Success() throws Exception {
        String highPriorityClaimJson = """
            {
                "claimNumber": "CLM-003",
                "policyNumber": "POL-11111",
                "claimType": "ILLNESS",
                "incidentDate": "2024-03-10",
                "claimedAmount": 8000.00,
                "description": "Medical emergency claim",
                "priority": "HIGH",
                "policyholderId": "PH-003",
                "policyholderName": "Bob Johnson",
                "policyholderEmail": "bob@email.com"
            }
            """;

        kafkaConsumerService.processHighPriorityClaim(highPriorityClaimJson);

        // Verify urgent claim was processed correctly
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-003")
                .orElseThrow(() -> new AssertionError("Urgent claim should be saved"));

        assertEquals(Claim.ClaimStatus.UNDER_REVIEW, savedClaim.getStatus());
        assertEquals(Claim.ClaimPriority.HIGH, savedClaim.getPriority());

        // Verify express assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-003")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for urgent claim"));

        assertEquals("EXPRESS_ASSESSOR", savedAssessment.getAssessorId());
        assertEquals(200, savedAssessment.getProcessingTimeMs());
        assertFalse(savedAssessment.getFraudFlag());
    }

    @Test
    public void testProcessClaimSubmission_WithHighPriorityNotification() throws Exception {
        String urgentClaimJson = """
            {
                "claimNumber": "CLM-004",
                "policyNumber": "POL-22222",
                "claimType": "NATURAL_DISASTER",
                "incidentDate": "2024-03-15",
                "claimedAmount": 15000.00,
                "description": "Flood damage claim",
                "priority": "URGENT",
                "policyholderId": "PH-004",
                "policyholderName": "Alice Brown",
                "policyholderEmail": "alice@email.com"
            }
            """;

        kafkaConsumerService.processClaimSubmission(urgentClaimJson);

        // Verify urgent claim was saved
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-004")
                .orElseThrow(() -> new AssertionError("Urgent claim should be saved"));

        assertEquals(Claim.ClaimPriority.URGENT, savedClaim.getPriority());
        assertEquals(Claim.ClaimStatus.SUBMITTED, savedClaim.getStatus());

        // Verify assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-004")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for urgent claim"));

        assertNotNull(savedAssessment);
        assertTrue(savedAssessment.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testProcessClaimSubmission_JsonParsingError() throws Exception {
        String invalidJson = """
            {
                "invalid": "json",
                "missing": "required fields"
            }
            """;

        // Should throw exception for invalid JSON
        assertThrows(RuntimeException.class, () -> {
            kafkaConsumerService.processClaimSubmission(invalidJson);
        });

        // Verify nothing was saved to database
        assertFalse(claimRepository.findByClaimNumber("invalid").isPresent());
        assertFalse(assessmentRepository.findByClaimNumber("invalid").isPresent());
    }

    @Test
    public void testProcessClaimSubmission_LowRiskClaim() throws Exception {
        String lowRiskClaimJson = """
            {
                "claimNumber": "CLM-005",
                "policyNumber": "POL-33333",
                "claimType": "ILLNESS",
                "incidentDate": "2024-03-20",
                "claimedAmount": 3000.00,
                "description": "Minor medical claim",
                "priority": "LOW",
                "policyholderId": "PH-005",
                "policyholderName": "Charlie Wilson",
                "policyholderEmail": "charlie@email.com"
            }
            """;

        kafkaConsumerService.processClaimSubmission(lowRiskClaimJson);

        // Verify low risk claim was processed
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-005")
                .orElseThrow(() -> new AssertionError("Low risk claim should be saved"));

        assertEquals(Claim.ClaimPriority.LOW, savedClaim.getPriority());
        assertEquals(new BigDecimal("3000.00"), savedClaim.getClaimedAmount());

        // Verify assessment was created with low risk score
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-005")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for low risk claim"));

        assertFalse(savedAssessment.getFraudFlag(), "Low risk claim should not be flagged as fraud");
        assertTrue(savedAssessment.getRiskScore() < 50, "Low risk score expected for low value claim");
    }

    @Test
    public void testMultipleClaimsProcessing() throws Exception {
        String[] claimJsons = {
            """
            {
                "claimNumber": "CLM-MULTI-001",
                "policyNumber": "POL-MULTI-001",
                "claimType": "ACCIDENT",
                "incidentDate": "2024-01-10",
                "claimedAmount": 3000.00,
                "description": "First test claim",
                "priority": "NORMAL",
                "policyholderId": "PH-M001",
                "policyholderName": "User One",
                "policyholderEmail": "user1@email.com"
            }
            """,
            """
            {
                "claimNumber": "CLM-MULTI-002",
                "policyNumber": "POL-MULTI-002",
                "claimType": "PROPERTY_DAMAGE",
                "incidentDate": "2024-01-11",
                "claimedAmount": 6000.00,
                "description": "Second test claim",
                "priority": "HIGH",
                "policyholderId": "PH-M002",
                "policyholderName": "User Two",
                "policyholderEmail": "user2@email.com"
            }
            """
        };

        // Process multiple claims
        for (String claimJson : claimJsons) {
            kafkaConsumerService.processClaimSubmission(claimJson);
        }

        // Verify all claims were saved
        assertTrue(claimRepository.findByClaimNumber("CLM-MULTI-001").isPresent(),
                "First claim should be saved");
        assertTrue(claimRepository.findByClaimNumber("CLM-MULTI-002").isPresent(),
                "Second claim should be saved");

        // Verify assessments were created
        assertTrue(assessmentRepository.findByClaimNumber("CLM-MULTI-001").isPresent(),
                "Assessment for first claim should be saved");
        assertTrue(assessmentRepository.findByClaimNumber("CLM-MULTI-002").isPresent(),
                "Assessment for second claim should be saved");

        // Verify final counts
        assertEquals(2, claimRepository.count(), "Should have exactly 2 claims saved");
        assertEquals(2, assessmentRepository.count(), "Should have exactly 2 assessments saved");
    }
}