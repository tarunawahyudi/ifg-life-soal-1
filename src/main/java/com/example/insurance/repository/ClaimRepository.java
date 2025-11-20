package com.example.insurance.repository;

import com.example.insurance.entity.Claim;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ClaimRepository implements PanacheRepositoryBase<Claim, Long> {

    @Inject
    EntityManager entityManager;

    public Optional<Claim> findByClaimNumber(String claimNumber) {
        return find("claimNumber", claimNumber).firstResultOptional();
    }

    public List<Claim> findByPolicyNumber(String policyNumber) {
        return find("policyNumber", policyNumber).list();
    }

    public List<Claim> findByStatus(Claim.ClaimStatus status) {
        return find("status", status).list();
    }

    public List<Claim> findByStatusAndPriority(Claim.ClaimStatus status, Claim.ClaimPriority priority) {
        return find("status = ?1 AND priority = ?2", status, priority).list();
    }

    public List<Claim> findHighPriorityClaims() {
        return find("priority IN (?1, ?2) AND status IN (?3, ?4)",
                  Claim.ClaimPriority.HIGH, Claim.ClaimPriority.URGENT,
                  Claim.ClaimStatus.SUBMITTED, Claim.ClaimStatus.UNDER_REVIEW).list();
    }

    public List<Claim> findPendingClaims() {
        return find("status IN (?1, ?2)", Claim.ClaimStatus.SUBMITTED, Claim.ClaimStatus.UNDER_REVIEW).list();
    }

    public List<Claim> findClaimsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return find("claimDate BETWEEN ?1 AND ?2", startDate, endDate).list();
    }

    @Transactional
    public Claim createOrUpdate(Claim claim) {
        if (claim.getId() == null) {
            persist(claim);
        } else {
            entityManager.merge(claim);
        }
        return claim;
    }

    @Transactional
    public boolean updateClaimStatus(String claimNumber, Claim.ClaimStatus newStatus) {
        return update("status = ?1 where claimNumber = ?2", newStatus, claimNumber) > 0;
    }

    @Transactional
    public boolean updateClaimPriority(String claimNumber, Claim.ClaimPriority newPriority) {
        return update("priority = ?1 where claimNumber = ?2", newPriority, claimNumber) > 0;
    }

    public long countByStatus(Claim.ClaimStatus status) {
        return count("status", status);
    }

    public long countByPriority(Claim.ClaimPriority priority) {
        return count("priority", priority);
    }

    public long countPendingClaims() {
        return count("status IN (?1, ?2)", Claim.ClaimStatus.SUBMITTED, Claim.ClaimStatus.UNDER_REVIEW);
    }
}