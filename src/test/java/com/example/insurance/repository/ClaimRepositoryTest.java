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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ClaimRepositoryTest {

    @Inject
    ClaimRepository claimRepository;

    @Inject
    ClaimAssessmentRepository assessmentRepository;

    private Claim testClaim1;
    private Claim testClaim2;
    private Claim testClaim3;

    @BeforeEach
    void setUp() {
        // No cleanup needed - use unique data for each test

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        testClaim1 = Claim.builder()
                .claimNumber("TEST-" + uniqueId)
                .policyNumber("POL-" + uniqueId)
                .claimType(Claim.ClaimType.ACCIDENT)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .claimedAmount(new BigDecimal("5000.00"))
                .description("Car accident test claim")
                .status(Claim.ClaimStatus.SUBMITTED)
                .priority(Claim.ClaimPriority.NORMAL)
                .build();

        testClaim2 = Claim.builder()
                .claimNumber("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .policyNumber("POL-" + UUID.randomUUID().toString().substring(0, 8))
                .claimType(Claim.ClaimType.ILLNESS)
                .incidentDate(LocalDate.of(2024, 2, 20))
                .claimedAmount(new BigDecimal("8000.00"))
                .description("Medical test claim")
                .status(Claim.ClaimStatus.UNDER_REVIEW)
                .priority(Claim.ClaimPriority.HIGH)
                .build();

        testClaim3 = Claim.builder()
                .claimNumber("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .policyNumber("POL-" + UUID.randomUUID().toString().substring(0, 8))
                .claimType(Claim.ClaimType.THEFT)
                .incidentDate(LocalDate.of(2024, 3, 10))
                .claimedAmount(new BigDecimal("12000.00"))
                .description("Theft test claim")
                .status(Claim.ClaimStatus.APPROVED)
                .priority(Claim.ClaimPriority.URGENT)
                .build();

        claimRepository.createOrUpdate(testClaim1);
        claimRepository.createOrUpdate(testClaim2);
        claimRepository.createOrUpdate(testClaim3);
    }

    @Test
    public void testCreateClaim_Success() {
        Claim newClaim = Claim.builder()
                .claimNumber("TEST-NEW")
                .policyNumber("POL-NEW")
                .claimType(Claim.ClaimType.PROPERTY_DAMAGE)
                .incidentDate(LocalDate.now())
                .claimedAmount(new BigDecimal("3000.00"))
                .description("New test claim")
                .status(Claim.ClaimStatus.SUBMITTED)
                .priority(Claim.ClaimPriority.NORMAL)
                .build();

        Claim savedClaim = claimRepository.createOrUpdate(newClaim);

        assertNotNull(savedClaim.getId());
        assertEquals("TEST-NEW", savedClaim.getClaimNumber());
        assertEquals("POL-NEW", savedClaim.getPolicyNumber());
        assertEquals(Claim.ClaimType.PROPERTY_DAMAGE, savedClaim.getClaimType());
        assertEquals(new BigDecimal("3000.00"), savedClaim.getClaimedAmount());
        assertEquals(Claim.ClaimStatus.SUBMITTED, savedClaim.getStatus());
        assertNotNull(savedClaim.getClaimDate());
        assertNotNull(savedClaim.getUpdatedAt());
    }

    @Test
    public void testUpdateClaim_Success() {
        Optional<Claim> foundClaim = claimRepository.findByClaimNumber("TEST-001");
        assertTrue(foundClaim.isPresent());

        Claim claimToUpdate = foundClaim.get();
        claimToUpdate.setStatus(Claim.ClaimStatus.UNDER_REVIEW);
        claimToUpdate.setDescription("Updated description");

        Claim updatedClaim = claimRepository.createOrUpdate(claimToUpdate);

        assertEquals(Claim.ClaimStatus.UNDER_REVIEW, updatedClaim.getStatus());
        assertEquals("Updated description", updatedClaim.getDescription());
        assertNotNull(updatedClaim.getUpdatedAt());
    }

    @Test
    public void testFindByClaimNumber_Success() {
        Optional<Claim> foundClaim = claimRepository.findByClaimNumber("TEST-001");
        assertTrue(foundClaim.isPresent());
        assertEquals("TEST-001", foundClaim.get().getClaimNumber());
        assertEquals("POL-001", foundClaim.get().getPolicyNumber());
        assertEquals(Claim.ClaimType.ACCIDENT, foundClaim.get().getClaimType());
    }

    @Test
    public void testFindByClaimNumber_NotFound() {
        Optional<Claim> foundClaim = claimRepository.findByClaimNumber("NON-EXISTENT");
        assertFalse(foundClaim.isPresent());
    }

    @Test
    public void testFindByPolicyNumber_Success() {
        List<Claim> claims = claimRepository.findByPolicyNumber("POL-002");
        assertEquals(1, claims.size());
        assertEquals("TEST-002", claims.get(0).getClaimNumber());
    }

    @Test
    public void testFindByPolicyNumber_EmptyResult() {
        List<Claim> claims = claimRepository.findByPolicyNumber("POL-NONEXISTENT");
        assertTrue(claims.isEmpty());
    }

    @Test
    public void testFindByStatus_Success() {
        List<Claim> submittedClaims = claimRepository.findByStatus(Claim.ClaimStatus.SUBMITTED);
        assertEquals(1, submittedClaims.size());
        assertEquals("TEST-001", submittedClaims.get(0).getClaimNumber());

        List<Claim> approvedClaims = claimRepository.findByStatus(Claim.ClaimStatus.APPROVED);
        assertEquals(1, approvedClaims.size());
        assertEquals("TEST-003", approvedClaims.get(0).getClaimNumber());
    }

    @Test
    public void testFindByStatusAndPriority_Success() {
        List<Claim> highPriorityUnderReview = claimRepository.findByStatusAndPriority(
                Claim.ClaimStatus.UNDER_REVIEW, Claim.ClaimPriority.HIGH);
        assertEquals(1, highPriorityUnderReview.size());
        assertEquals("TEST-002", highPriorityUnderReview.get(0).getClaimNumber());

        List<Claim> normalPrioritySubmitted = claimRepository.findByStatusAndPriority(
                Claim.ClaimStatus.SUBMITTED, Claim.ClaimPriority.NORMAL);
        assertEquals(1, normalPrioritySubmitted.size());
        assertEquals("TEST-001", normalPrioritySubmitted.get(0).getClaimNumber());
    }

    @Test
    public void testFindHighPriorityClaims_Success() {
        List<Claim> highPriorityClaims = claimRepository.findHighPriorityClaims();
        assertEquals(1, highPriorityClaims.size());
        assertEquals("TEST-002", highPriorityClaims.get(0).getClaimNumber());
    }

    @Test
    public void testFindPendingClaims_Success() {
        List<Claim> pendingClaims = claimRepository.findPendingClaims();
        assertEquals(2, pendingClaims.size());

        List<String> claimNumbers = pendingClaims.stream()
                .map(Claim::getClaimNumber)
                .toList();
        assertTrue(claimNumbers.contains("TEST-001"));
        assertTrue(claimNumbers.contains("TEST-002"));
    }

    @Test
    public void testFindClaimsByDateRange_Success() {
        LocalDateTime startDate = LocalDate.of(2024, 1, 1).atStartOfDay();
        LocalDateTime endDate = LocalDate.of(2024, 2, 28).atStartOfDay();

        List<Claim> claimsInRange = claimRepository.findClaimsByDateRange(startDate, endDate);
        assertEquals(2, claimsInRange.size());

        List<String> claimNumbers = claimsInRange.stream()
                .map(Claim::getClaimNumber)
                .toList();
        assertTrue(claimNumbers.contains("TEST-001"));
        assertTrue(claimNumbers.contains("TEST-002"));
        assertFalse(claimNumbers.contains("TEST-003"));
    }

    @Test
    public void testUpdateClaimStatus_Success() {
        boolean updated = claimRepository.updateClaimStatus("TEST-001", Claim.ClaimStatus.APPROVED);
        assertTrue(updated);

        Optional<Claim> updatedClaim = claimRepository.findByClaimNumber("TEST-001");
        assertTrue(updatedClaim.isPresent());
        assertEquals(Claim.ClaimStatus.APPROVED, updatedClaim.get().getStatus());
    }

    @Test
    public void testUpdateClaimStatus_NotFound() {
        boolean updated = claimRepository.updateClaimStatus("NON-EXISTENT", Claim.ClaimStatus.APPROVED);
        assertFalse(updated);
    }

    @Test
    public void testUpdateClaimPriority_Success() {
        boolean updated = claimRepository.updateClaimPriority("TEST-001", Claim.ClaimPriority.HIGH);
        assertTrue(updated);

        Optional<Claim> updatedClaim = claimRepository.findByClaimNumber("TEST-001");
        assertTrue(updatedClaim.isPresent());
        assertEquals(Claim.ClaimPriority.HIGH, updatedClaim.get().getPriority());
    }

    @Test
    public void testUpdateClaimPriority_NotFound() {
        boolean updated = claimRepository.updateClaimPriority("NON-EXISTENT", Claim.ClaimPriority.HIGH);
        assertFalse(updated);
    }

    @Test
    public void testCountByStatus_Success() {
        long submittedCount = claimRepository.countByStatus(Claim.ClaimStatus.SUBMITTED);
        assertEquals(1, submittedCount);

        long underReviewCount = claimRepository.countByStatus(Claim.ClaimStatus.UNDER_REVIEW);
        assertEquals(1, underReviewCount);

        long approvedCount = claimRepository.countByStatus(Claim.ClaimStatus.APPROVED);
        assertEquals(1, approvedCount);
    }

    @Test
    public void testCountByPriority_Success() {
        long normalCount = claimRepository.countByPriority(Claim.ClaimPriority.NORMAL);
        assertEquals(1, normalCount);

        long highCount = claimRepository.countByPriority(Claim.ClaimPriority.HIGH);
        assertEquals(1, highCount);

        long urgentCount = claimRepository.countByPriority(Claim.ClaimPriority.URGENT);
        assertEquals(1, urgentCount);
    }

    @Test
    public void testCountPendingClaims_Success() {
        long pendingCount = claimRepository.countPendingClaims();
        assertEquals(2, pendingCount);
    }

    @Test
    public void testListAllClaims_Success() {
        List<Claim> allClaims = claimRepository.listAll();
        assertEquals(3, allClaims.size());

        List<String> claimNumbers = allClaims.stream()
                .map(Claim::getClaimNumber)
                .toList();
        assertTrue(claimNumbers.contains("TEST-001"));
        assertTrue(claimNumbers.contains("TEST-002"));
        assertTrue(claimNumbers.contains("TEST-003"));
    }

    @Test
    @Transactional
    public void testClaimConstraints_UniqueClaimNumber() {
        Claim duplicateClaim = Claim.builder()
                .claimNumber("TEST-001")
                .policyNumber("POL-DUPLICATE")
                .claimType(Claim.ClaimType.DISABILITY)
                .incidentDate(LocalDate.now())
                .claimedAmount(new BigDecimal("2000.00"))
                .description("Duplicate claim number test")
                .status(Claim.ClaimStatus.SUBMITTED)
                .priority(Claim.ClaimPriority.NORMAL)
                .build();

        assertThrows(Exception.class, () -> {
            claimRepository.createOrUpdate(duplicateClaim);
        });
    }

    @Test
    @Transactional
    public void testClaimConstraints_NotNullFields() {
        Claim invalidClaim = Claim.builder()
                .claimNumber(null)
                .policyNumber(null)
                .claimType(null)
                .incidentDate(null)
                .claimedAmount(null)
                .description(null)
                .status(null)
                .priority(null)
                .build();

        assertThrows(Exception.class, () -> {
            claimRepository.createOrUpdate(invalidClaim);
        });
    }
}