package com.example.insurance.repository;

import com.example.insurance.entity.ClaimAssessment;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ClaimAssessmentRepository implements PanacheRepositoryBase<ClaimAssessment, Long> {

    @Inject
    EntityManager entityManager;

    public Optional<ClaimAssessment> findByClaimNumber(String claimNumber) {
        return find("claimNumber", claimNumber).firstResultOptional();
    }

    public List<ClaimAssessment> findByAssessorId(String assessorId) {
        return find("assessorId", assessorId).list();
    }

    public List<ClaimAssessment> findFraudulentClaims() {
        return find("fraudFlag = true").list();
    }

    public List<ClaimAssessment> findByRiskScoreRange(Integer minScore, Integer maxScore) {
        return find("riskScore BETWEEN ?1 AND ?2", minScore, maxScore).list();
    }

    public List<ClaimAssessment> findHighRiskClaims(int threshold) {
        return find("riskScore >= ?1", threshold).list();
    }

    public List<ClaimAssessment> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return find("assessmentDate BETWEEN ?1 AND ?2", startDate, endDate).list();
    }

    @Transactional
    public ClaimAssessment create(ClaimAssessment assessment) {
        persist(assessment);
        return assessment;
    }

    @Transactional
    public void createOrUpdate(ClaimAssessment assessment) {
        if (assessment.getId() == null) {
            persist(assessment);
        } else {
            entityManager.merge(assessment);
        }
    }

    public long countFraudulentClaims() {
        return count("fraudFlag", true);
    }

    public long countHighRiskClaims(int threshold) {
        return count("riskScore >= ?1", threshold);
    }

    public double getAverageProcessingTime() {
        Number avgTime = (Number) getEntityManager()
                .createQuery("SELECT AVG(processingTimeMs) FROM ClaimAssessment WHERE processingTimeMs IS NOT NULL")
                .getSingleResult();
        return avgTime != null ? avgTime.doubleValue() : 0.0;
    }

    public Double getAverageRiskScore() {
        Number avgScore = (Number) getEntityManager()
                .createQuery("SELECT AVG(riskScore) FROM ClaimAssessment WHERE riskScore IS NOT NULL")
                .getSingleResult();
        return avgScore != null ? avgScore.doubleValue() : 0.0;
    }

    public List<ClaimAssessment> findByFraudFlag(boolean fraudFlag) {
        return find("fraudFlag", fraudFlag).list();
    }

    public List<ClaimAssessment> findHighRiskAssessments() {
        return find("riskScore >= 40", 40).list();
    }

    public long countByAssessorId(String assessorId) {
        return count("assessorId", assessorId);
    }

    public long countByFraudFlag(boolean fraudFlag) {
        return count("fraudFlag", fraudFlag);
    }

    public long countHighRiskAssessments() {
        return count("riskScore >= 40");
    }

    @Transactional
    public boolean deleteByClaimNumber(String claimNumber) {
        return delete("claimNumber", claimNumber) > 0;
    }
}