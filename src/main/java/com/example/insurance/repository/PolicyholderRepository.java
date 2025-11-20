package com.example.insurance.repository;

import com.example.insurance.entity.Policyholder;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PolicyholderRepository implements PanacheRepositoryBase<Policyholder, Long> {

    @Inject
    EntityManager entityManager;

    public Optional<Policyholder> findByPolicyholderId(String policyholderId) {
        return find("policyholderId", policyholderId).firstResultOptional();
    }

    public Optional<Policyholder> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public List<Policyholder> findActivePolicyholders() {
        return find("isActive", true).list();
    }

    @Transactional
    public Policyholder createOrUpdate(Policyholder policyholder) {
        if (policyholder.getId() == null) {
            persist(policyholder);
        } else {
            entityManager.merge(policyholder);
        }
        return policyholder;
    }

    @Transactional
    public boolean deleteByPolicyholderId(String policyholderId) {
        return delete("policyholderId", policyholderId) > 0;
    }

    public long countActivePolicyholders() {
        return count("isActive", true);
    }
}