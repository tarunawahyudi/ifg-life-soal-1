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
public class KafkaIntegrationTest {

    @Inject
    KafkaClaimConsumerService kafkaConsumerService;

    @Inject
    ClaimProcessorService claimProcessorService;

    @Inject
    ClaimAssessmentService assessmentService;

    @Inject
    KafkaProducerService kafkaProducerService;

    @Inject
    ClaimRepository claimRepository;

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        assessmentRepository.deleteAll();
        claimRepository.deleteAll();
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

        // Process the claim through consumer service
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
    public void testProcessHighPriorityClaim_Success() throws Exception {
        String highPriorityClaimJson = """
            {
                "claimNumber": "CLM-HIGH-001",
                "policyNumber": "POL-HIGH-001",
                "claimType": "ILLNESS",
                "incidentDate": "2024-03-10",
                "claimedAmount": 8000.00,
                "description": "Medical emergency claim",
                "priority": "HIGH",
                "policyholderId": "PH-HIGH-001",
                "policyholderName": "Bob Johnson",
                "policyholderEmail": "bob@email.com"
            }
            """;

        kafkaConsumerService.processHighPriorityClaim(highPriorityClaimJson);

        // Verify urgent claim was processed correctly
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-HIGH-001")
                .orElseThrow(() -> new AssertionError("Urgent claim should be saved"));

        assertEquals(Claim.ClaimStatus.UNDER_REVIEW, savedClaim.getStatus());
        assertEquals(Claim.ClaimPriority.HIGH, savedClaim.getPriority());

        // Verify express assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-HIGH-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for urgent claim"));

        assertEquals("EXPRESS_ASSESSOR", savedAssessment.getAssessorId());
        assertEquals(200, savedAssessment.getProcessingTimeMs());
        assertFalse(savedAssessment.getFraudFlag());
    }

    @Test
    public void testClaimProcessorService_DirectProcessing() {
        ClaimSubmission claimSubmission = new ClaimSubmission();
        claimSubmission.setClaimNumber("CLM-DIRECT-001");
        claimSubmission.setPolicyNumber("POL-DIRECT-001");
        claimSubmission.setClaimType(Claim.ClaimType.ACCIDENT);
        claimSubmission.setIncidentDate(LocalDate.of(2024, 1, 15));
        claimSubmission.setClaimedAmount(new BigDecimal("5000.00"));
        claimSubmission.setDescription("Direct processing test");
        claimSubmission.setPriority(Claim.ClaimPriority.NORMAL);
        claimSubmission.setPolicyholderId("PH-DIRECT-001");
        claimSubmission.setPolicyholderName("Test User");
        claimSubmission.setPolicyholderEmail("test@email.com");

        // Process directly through processor service
        claimProcessorService.processClaimSubmission(claimSubmission);

        // Verify claim was saved
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-DIRECT-001")
                .orElseThrow(() -> new AssertionError("Claim should be saved"));

        assertNotNull(savedClaim);
        assertEquals("CLM-DIRECT-001", savedClaim.getClaimNumber());
        assertEquals(Claim.ClaimStatus.SUBMITTED, savedClaim.getStatus());

        // Verify assessment was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-DIRECT-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved"));

        assertNotNull(savedAssessment);
        assertEquals("CLM-DIRECT-001", savedAssessment.getClaimNumber());
    }

    @Test
    public void testClaimAssessmentService_StandardAssessment() {
        Claim claim = Claim.builder()
                .claimNumber("CLM-ASSESS-001")
                .policyNumber("POL-ASSESS-001")
                .claimType(Claim.ClaimType.ACCIDENT)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .claimedAmount(new BigDecimal("5000.00"))
                .description("Assessment test claim")
                .priority(Claim.ClaimPriority.NORMAL)
                .status(Claim.ClaimStatus.SUBMITTED)
                .build();

        // Test assessment service directly
        ClaimAssessment assessment = assessmentService.performClaimAssessment(claim);

        assertNotNull(assessment);
        assertEquals("CLM-ASSESS-001", assessment.getClaimNumber());
        assertNotNull(assessment.getAssessorId());
        assertTrue(assessment.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0);
        assertFalse(assessment.getFraudFlag());
        assertNotNull(assessment.getAssessmentNotes());
        assertTrue(assessment.getProcessingTimeMs() > 0);
    }

    @Test
    public void testClaimAssessmentService_ExpressAssessment() {
        Claim claim = Claim.builder()
                .claimNumber("CLM-EXPRESS-001")
                .policyNumber("POL-EXPRESS-001")
                .claimType(Claim.ClaimType.ILLNESS)
                .incidentDate(LocalDate.of(2024, 3, 10))
                .claimedAmount(new BigDecimal("8000.00"))
                .description("Express assessment test")
                .priority(Claim.ClaimPriority.HIGH)
                .status(Claim.ClaimStatus.UNDER_REVIEW)
                .build();

        // Test express assessment
        ClaimAssessment assessment = assessmentService.performExpressAssessment(claim);

        assertNotNull(assessment);
        assertEquals("CLM-EXPRESS-001", assessment.getClaimNumber());
        assertEquals("EXPRESS_ASSESSOR", assessment.getAssessorId());
        assertEquals(200, assessment.getProcessingTimeMs());
        assertFalse(assessment.getFraudFlag());
    }

    @Test
    public void testKafkaProducerService_PublishMethods() {
        ClaimSubmission testClaim = new ClaimSubmission();
        testClaim.setClaimNumber("TEST-PUB-001");
        testClaim.setPolicyNumber("POL-PUB-001");
        testClaim.setClaimType(Claim.ClaimType.ACCIDENT);
        testClaim.setIncidentDate(LocalDate.of(2024, 1, 15));
        testClaim.setClaimedAmount(new BigDecimal("5000.00"));
        testClaim.setDescription("Test claim for publishing");
        testClaim.setPriority(Claim.ClaimPriority.NORMAL);
        testClaim.setPolicyholderId("PH-PUB-001");
        testClaim.setPolicyholderName("Test User");
        testClaim.setPolicyholderEmail("test@email.com");

        ClaimSubmission urgentClaim = new ClaimSubmission();
        urgentClaim.setClaimNumber("TEST-PUB-002");
        urgentClaim.setPolicyNumber("POL-PUB-002");
        urgentClaim.setClaimType(Claim.ClaimType.ILLNESS);
        urgentClaim.setIncidentDate(LocalDate.of(2024, 1, 15));
        urgentClaim.setClaimedAmount(new BigDecimal("8000.00"));
        urgentClaim.setDescription("Test urgent claim for publishing");
        urgentClaim.setPriority(Claim.ClaimPriority.URGENT);
        urgentClaim.setPolicyholderId("PH-PUB-002");
        urgentClaim.setPolicyholderName("Urgent User");
        urgentClaim.setPolicyholderEmail("urgent@email.com");

        // Test producer publish methods - these should not throw exceptions
        assertDoesNotThrow(() -> {
            kafkaProducerService.publishClaimSubmission(testClaim);
        });

        assertDoesNotThrow(() -> {
            kafkaProducerService.publishHighPriorityClaim(urgentClaim);
        });
    }

    @Test
    public void testProcessClaimSubmission_WithFraudDetection() throws Exception {
        String highRiskClaimJson = """
            {
                "claimNumber": "CLM-FRAUD-001",
                "policyNumber": "POL-FRAUD-001",
                "claimType": "THEFT",
                "incidentDate": "2024-02-20",
                "claimedAmount": 60000.00,
                "description": "High value theft claim",
                "priority": "HIGH",
                "policyholderId": "PH-FRAUD-001",
                "policyholderName": "Jane Smith",
                "policyholderEmail": "jane@email.com"
            }
            """;

        kafkaConsumerService.processClaimSubmission(highRiskClaimJson);

        // Verify claim was saved
        Claim savedClaim = claimRepository.findByClaimNumber("CLM-FRAUD-001")
                .orElseThrow(() -> new AssertionError("High risk claim should be saved"));

        assertEquals("CLM-FRAUD-001", savedClaim.getClaimNumber());
        assertEquals(Claim.ClaimType.THEFT, savedClaim.getClaimType());
        assertEquals(new BigDecimal("60000.00"), savedClaim.getClaimedAmount());
        assertEquals(Claim.ClaimPriority.HIGH, savedClaim.getPriority());

        // Verify assessment with fraud detection was created
        ClaimAssessment savedAssessment = assessmentRepository.findByClaimNumber("CLM-FRAUD-001")
                .orElseThrow(() -> new AssertionError("Assessment should be saved for high risk claim"));

        assertNotNull(savedAssessment);
        assertTrue(savedAssessment.getFraudFlag(), "High value theft claim should be flagged as fraud");
        assertTrue(savedAssessment.getRiskScore() > 70, "High risk score expected for fraud claim");
        assertTrue(savedAssessment.getAssessmentNotes().toLowerCase().contains("fraud"));
    }
}