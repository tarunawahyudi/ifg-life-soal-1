package com.example.insurance.repository;

import com.example.insurance.entity.InsurancePolicy;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InsurancePolicyRepository implements PanacheRepositoryBase<InsurancePolicy, Long> {

    @Inject
    EntityManager entityManager;

    public Optional<InsurancePolicy> findByPolicyNumber(String policyNumber) {
        return find("policyNumber", policyNumber).firstResultOptional();
    }

    public List<InsurancePolicy> findByPolicyholderId(String policyholderId) {
        return find("policyholderId", policyholderId).list();
    }

    public List<InsurancePolicy> findActivePolicies() {
        return find("status", InsurancePolicy.PolicyStatus.ACTIVE).list();
    }

    public List<InsurancePolicy> findActivePoliciesByPolicyholder(String policyholderId) {
        return find("policyholderId = ?1 AND status = ?2", policyholderId, InsurancePolicy.PolicyStatus.ACTIVE).list();
    }

    public List<InsurancePolicy> findExpiringPolicies(int daysFromNow) {
        LocalDate endDate = LocalDate.now().plusDays(daysFromNow);
        return find("endDate <= ?1 AND status = ?2", endDate, InsurancePolicy.PolicyStatus.ACTIVE).list();
    }

    @Transactional
    public InsurancePolicy createOrUpdate(InsurancePolicy policy) {
        if (policy.getId() == null) {
            persist(policy);
        } else {
            entityManager.merge(policy);
        }
        return policy;
    }

    @Transactional
    public boolean updatePolicyStatus(String policyNumber, InsurancePolicy.PolicyStatus newStatus) {
        return update("status = ?1 where policyNumber = ?2", newStatus, policyNumber) > 0;
    }

    public long countActivePolicies() {
        return count("status", InsurancePolicy.PolicyStatus.ACTIVE);
    }

    public long countPoliciesByType(InsurancePolicy.PolicyType policyType) {
        return count("policyType", policyType);
    }
}