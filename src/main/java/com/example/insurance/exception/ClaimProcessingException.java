package com.example.insurance.exception;

public class ClaimProcessingException extends RuntimeException {
    private final String claimNumber;
    private final String errorCode;

    public ClaimProcessingException(String message) {
        super(message);
        this.claimNumber = null;
        this.errorCode = "CLAIM_PROCESSING_ERROR";
    }

    public ClaimProcessingException(String claimNumber, String message) {
        super(message);
        this.claimNumber = claimNumber;
        this.errorCode = "CLAIM_PROCESSING_ERROR";
    }

    public ClaimProcessingException(String claimNumber, String message, String errorCode) {
        super(message);
        this.claimNumber = claimNumber;
        this.errorCode = errorCode;
    }

    public ClaimProcessingException(String claimNumber, String message, Throwable cause) {
        super(message, cause);
        this.claimNumber = claimNumber;
        this.errorCode = "CLAIM_PROCESSING_ERROR";
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getErrorCode() {
        return errorCode;
    }
}