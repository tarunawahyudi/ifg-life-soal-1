package com.example.insurance.dto;

import com.example.insurance.entity.Claim;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClaimSubmission {

    private String claimNumber;

    @NotBlank(message = "Policy number is required")
    @Size(max = 50, message = "Policy number must not exceed 50 characters")
    private String policyNumber;

    @NotNull(message = "Claim type is required")
    private Claim.ClaimType claimType;

    @NotNull(message = "Incident date is required")
    @PastOrPresent(message = "Incident date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate incidentDate;

    @NotNull(message = "Claimed amount is required")
    @DecimalMin(value = "0.01", message = "Claimed amount must be greater than 0")
    private BigDecimal claimedAmount;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Claim.ClaimPriority priority = Claim.ClaimPriority.NORMAL;

    private String policyholderId;
    private String policyholderName;
    private String policyholderEmail;

    // Constructors
    public ClaimSubmission() {}

    // Getters and Setters
    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Claim.ClaimType getClaimType() {
        return claimType;
    }

    public void setClaimType(Claim.ClaimType claimType) {
        this.claimType = claimType;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDate incidentDate) {
        this.incidentDate = incidentDate;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public void setClaimedAmount(BigDecimal claimedAmount) {
        this.claimedAmount = claimedAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Claim.ClaimPriority getPriority() {
        return priority;
    }

    public void setPriority(Claim.ClaimPriority priority) {
        this.priority = priority;
    }

    public String getPolicyholderId() {
        return policyholderId;
    }

    public void setPolicyholderId(String policyholderId) {
        this.policyholderId = policyholderId;
    }

    public String getPolicyholderName() {
        return policyholderName;
    }

    public void setPolicyholderName(String policyholderName) {
        this.policyholderName = policyholderName;
    }

    public String getPolicyholderEmail() {
        return policyholderEmail;
    }

    public void setPolicyholderEmail(String policyholderEmail) {
        this.policyholderEmail = policyholderEmail;
    }
}