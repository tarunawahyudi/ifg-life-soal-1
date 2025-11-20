package com.example.insurance.repository;

import com.example.insurance.entity.Claim;
import com.example.insurance.entity.ClaimAssessment;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ClaimAssessmentRepositoryTest {

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    @Inject
    ClaimRepository claimRepository;

    private Claim testClaim;
    private ClaimAssessment testAssessment1;
    private ClaimAssessment testAssessment2;

    @BeforeEach
    void setUp() {
        // No cleanup needed - use unique data for each test

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        testClaim = Claim.builder()
                .claimNumber("ASSESS-" + uniqueId)
                .policyNumber("POL-" + uniqueId)
                .claimType(Claim.ClaimType.ACCIDENT)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .claimedAmount(new BigDecimal("5000.00"))
                .description("Test claim for assessment")
                .status(Claim.ClaimStatus.SUBMITTED)
                .priority(Claim.ClaimPriority.NORMAL)
                .build();

        claimRepository.createOrUpdate(testClaim);

        testAssessment1 = ClaimAssessment.builder()
                .claimNumber("ASSESS-TEST-001")
                .assessorId("ASSESSOR-001")
                .approvedAmount(new BigDecimal("4250.00"))
                .riskScore(25)
                .fraudFlag(false)
                .assessmentNotes("Standard assessment")
                .processingTimeMs(500)
                .build();

        testAssessment2 = ClaimAssessment.builder()
                .claimNumber("ASSESS-TEST-002")
                .assessorId("ASSESSOR-002")
                .approvedAmount(new BigDecimal("6000.00"))
                .riskScore(45)
                .fraudFlag(true)
                .assessmentNotes("High risk assessment - fraud flagged")
                .processingTimeMs(750)
                .build();

        assessmentRepository.create(testAssessment1);
        assessmentRepository.create(testAssessment2);
    }

    @Test
    public void testCreateAssessment_Success() {
        Claim newClaim = Claim.builder()
                .claimNumber("ASSESS-TEST-003")
                .policyNumber("POL-ASSESS-003")
                .claimType(Claim.ClaimType.ILLNESS)
                .incidentDate(LocalDate.now())
                .claimedAmount(new BigDecimal("8000.00"))
                .description("New claim for assessment")
                .status(Claim.ClaimStatus.SUBMITTED)
                .priority(Claim.ClaimPriority.NORMAL)
                .build();

        claimRepository.createOrUpdate(newClaim);

        ClaimAssessment newAssessment = ClaimAssessment.builder()
                .claimNumber("ASSESS-TEST-003")
                .assessorId("ASSESSOR-NEW")
                .approvedAmount(new BigDecimal("7200.00"))
                .riskScore(30)
                .fraudFlag(false)
                .assessmentNotes("New assessment record")
                .processingTimeMs(400)
                .build();

        assessmentRepository.create(newAssessment);

        Optional<ClaimAssessment> foundAssessment = assessmentRepository.findByClaimNumber("ASSESS-TEST-003");
        assertTrue(foundAssessment.isPresent());
        assertEquals("ASSESSOR-NEW", foundAssessment.get().getAssessorId());
        assertEquals(new BigDecimal("7200.00"), foundAssessment.get().getApprovedAmount());
        assertEquals(30, foundAssessment.get().getRiskScore());
        assertFalse(foundAssessment.get().getFraudFlag());
    }

    @Test
    public void testFindByClaimNumber_Success() {
        Optional<ClaimAssessment> foundAssessment = assessmentRepository.findByClaimNumber("ASSESS-TEST-001");
        assertTrue(foundAssessment.isPresent());
        assertEquals("ASSESSOR-001", foundAssessment.get().getAssessorId());
        assertEquals(new BigDecimal("4250.00"), foundAssessment.get().getApprovedAmount());
        assertEquals(25, foundAssessment.get().getRiskScore());
        assertFalse(foundAssessment.get().getFraudFlag());
    }

    @Test
    public void testFindByClaimNumber_NotFound() {
        Optional<ClaimAssessment> foundAssessment = assessmentRepository.findByClaimNumber("NON-EXISTENT");
        assertFalse(foundAssessment.isPresent());
    }

    @Test
    public void testFindByAssessorId_Success() {
        List<ClaimAssessment> assessments = assessmentRepository.findByAssessorId("ASSESSOR-001");
        assertEquals(1, assessments.size());
        assertEquals("ASSESS-TEST-001", assessments.get(0).getClaimNumber());
    }

    @Test
    public void testFindByAssessorId_EmptyResult() {
        List<ClaimAssessment> assessments = assessmentRepository.findByAssessorId("NON-EXISTENT-ASSESSOR");
        assertTrue(assessments.isEmpty());
    }

    @Test
    public void testFindByFraudFlag_True() {
        List<ClaimAssessment> fraudAssessments = assessmentRepository.findByFraudFlag(true);
        assertEquals(1, fraudAssessments.size());
        assertEquals("ASSESS-TEST-002", fraudAssessments.get(0).getClaimNumber());
        assertTrue(fraudAssessments.get(0).getFraudFlag());
        assertEquals("ASSESSOR-002", fraudAssessments.get(0).getAssessorId());
    }

    @Test
    public void testFindByFraudFlag_False() {
        List<ClaimAssessment> nonFraudAssessments = assessmentRepository.findByFraudFlag(false);
        assertEquals(1, nonFraudAssessments.size());
        assertEquals("ASSESS-TEST-001", nonFraudAssessments.get(0).getClaimNumber());
        assertFalse(nonFraudAssessments.get(0).getFraudFlag());
    }

    @Test
    public void testFindHighRiskAssessments_Success() {
        List<ClaimAssessment> highRiskAssessments = assessmentRepository.findHighRiskAssessments();
        assertEquals(1, highRiskAssessments.size());
        assertEquals("ASSESS-TEST-002", highRiskAssessments.get(0).getClaimNumber());
        assertEquals(45, highRiskAssessments.get(0).getRiskScore());
    }

    @Test
    public void testFindByRiskScoreRange_Success() {
        List<ClaimAssessment> mediumRiskAssessments = assessmentRepository.findByRiskScoreRange(20, 30);
        assertEquals(1, mediumRiskAssessments.size());
        assertEquals("ASSESS-TEST-001", mediumRiskAssessments.get(0).getClaimNumber());
        assertEquals(25, mediumRiskAssessments.get(0).getRiskScore());

        List<ClaimAssessment> highRiskAssessments = assessmentRepository.findByRiskScoreRange(40, 50);
        assertEquals(1, highRiskAssessments.size());
        assertEquals("ASSESS-TEST-002", highRiskAssessments.get(0).getClaimNumber());
        assertEquals(45, highRiskAssessments.get(0).getRiskScore());
    }

    @Test
    public void testFindByRiskScoreRange_NoResults() {
        List<ClaimAssessment> noRiskAssessments = assessmentRepository.findByRiskScoreRange(60, 70);
        assertTrue(noRiskAssessments.isEmpty());
    }

    @Test
    public void testCountByAssessorId_Success() {
        long count = assessmentRepository.countByAssessorId("ASSESSOR-001");
        assertEquals(1, count);

        long countForNonExistent = assessmentRepository.countByAssessorId("NON-EXISTENT");
        assertEquals(0, countForNonExistent);
    }

    @Test
    public void testCountByFraudFlag_Success() {
        long fraudCount = assessmentRepository.countByFraudFlag(true);
        assertEquals(1, fraudCount);

        long nonFraudCount = assessmentRepository.countByFraudFlag(false);
        assertEquals(1, nonFraudCount);
    }

    @Test
    public void testCountHighRiskAssessments_Success() {
        long highRiskCount = assessmentRepository.countHighRiskAssessments();
        assertEquals(1, highRiskCount);
    }

    @Test
    public void testFindAllAssessments_Success() {
        List<ClaimAssessment> allAssessments = assessmentRepository.findAll().list();
        assertEquals(2, allAssessments.size());

        List<String> claimNumbers = allAssessments.stream()
                .map(ClaimAssessment::getClaimNumber)
                .toList();
        assertTrue(claimNumbers.contains("ASSESS-TEST-001"));
        assertTrue(claimNumbers.contains("ASSESS-TEST-002"));
    }

    @Test
    public void testDeleteByClaimNumber_Success() {
        boolean deleted = assessmentRepository.deleteByClaimNumber("ASSESS-TEST-001");
        assertTrue(deleted);

        Optional<ClaimAssessment> foundAssessment = assessmentRepository.findByClaimNumber("ASSESS-TEST-001");
        assertFalse(foundAssessment.isPresent());

        List<ClaimAssessment> remainingAssessments = assessmentRepository.findAll().list();
        assertEquals(1, remainingAssessments.size());
        assertEquals("ASSESS-TEST-002", remainingAssessments.get(0).getClaimNumber());
    }

    @Test
    public void testDeleteByClaimNumber_NotFound() {
        boolean deleted = assessmentRepository.deleteByClaimNumber("NON-EXISTENT");
        assertFalse(deleted);

        List<ClaimAssessment> assessments = assessmentRepository.findAll().list();
        assertEquals(2, assessments.size());
    }

    @Test
    public void testAssessmentConstraints_NotNullFields() {
        ClaimAssessment invalidAssessment = ClaimAssessment.builder()
                .claimNumber(null)
                .assessorId(null)
                .approvedAmount(null)
                .riskScore(null)
                .fraudFlag(null)
                .assessmentNotes(null)
                .processingTimeMs(null)
                .build();

        assertThrows(Exception.class, () -> {
            assessmentRepository.create(invalidAssessment);
        });
    }

    @Test
    @Transactional
    public void testAssessmentRelationship_WithClaim() {
        Optional<Claim> foundClaim = claimRepository.findByClaimNumber("ASSESS-TEST-001");
        assertTrue(foundClaim.isPresent());

        Optional<ClaimAssessment> foundAssessment = assessmentRepository.findByClaimNumber("ASSESS-TEST-001");
        assertTrue(foundAssessment.isPresent());

        assertEquals(foundClaim.get().getClaimNumber(), foundAssessment.get().getClaimNumber());
        assertEquals(foundClaim.get().getClaimedAmount(), new BigDecimal("5000.00"));
        assertTrue(foundAssessment.get().getApprovedAmount().compareTo(foundClaim.get().getClaimedAmount()) < 0);
    }

    @Test
    @Transactional
    public void testUpdateAssessment_Success() {
        Optional<ClaimAssessment> foundAssessment = assessmentRepository.findByClaimNumber("ASSESS-TEST-001");
        assertTrue(foundAssessment.isPresent());

        ClaimAssessment assessmentToUpdate = foundAssessment.get();
        assessmentToUpdate.setApprovedAmount(new BigDecimal("4500.00"));
        assessmentToUpdate.setRiskScore(30);
        assessmentToUpdate.setAssessmentNotes("Updated assessment notes");

        assessmentRepository.createOrUpdate(assessmentToUpdate);

        Optional<ClaimAssessment> updatedAssessment = assessmentRepository.findByClaimNumber("ASSESS-TEST-001");
        assertTrue(updatedAssessment.isPresent());
        assertEquals(new BigDecimal("4500.00"), updatedAssessment.get().getApprovedAmount());
        assertEquals(30, updatedAssessment.get().getRiskScore());
        assertEquals("Updated assessment notes", updatedAssessment.get().getAssessmentNotes());
    }
}