package com.example.insurance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaimSubmissionResponse {
    private String claimNumber;
    private String policyNumber;
    private String status;
    private String message;

    public ClaimSubmissionResponse() {}

    public ClaimSubmissionResponse(String claimNumber, String policyNumber, String status, String message) {
        this.claimNumber = claimNumber;
        this.policyNumber = policyNumber;
        this.status = status;
        this.message = message;
    }

    public static ClaimSubmissionResponse accepted(String claimNumber, String policyNumber) {
        return new ClaimSubmissionResponse(
            claimNumber,
            policyNumber,
            "ACCEPTED",
            "Claim submitted successfully for processing"
        );
    }

    public static ClaimSubmissionResponse urgentAccepted(String claimNumber, String policyNumber) {
        return new ClaimSubmissionResponse(
            claimNumber,
            policyNumber,
            "URGENT_ACCEPTED",
            "Urgent claim submitted successfully for expedited processing"
        );
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}