package com.example.insurance.rest;

import com.example.insurance.dto.ApiResponse;
import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.dto.ClaimSubmissionResponse;
import com.example.insurance.exception.ClaimProcessingException;
import com.example.insurance.service.KafkaProducerService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/claims")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClaimSubmissionResource {

    @Inject
    KafkaProducerService kafkaProducerService;

    @POST
    @Path("/submit")
    public Response submitClaim(@Valid ClaimSubmission claimSubmission) {
        Log.info("[CLAIM-API] Submitting claim for policy: " + claimSubmission.getPolicyNumber());

        try {
            kafkaProducerService.publishClaimSubmission(claimSubmission);

            ClaimSubmissionResponse response = ClaimSubmissionResponse.accepted(
                claimSubmission.getClaimNumber(),
                claimSubmission.getPolicyNumber()
            );

            ApiResponse<ClaimSubmissionResponse> apiResponse = ApiResponse.success(
                "Claim submitted successfully for processing",
                response
            );

            return Response.status(Response.Status.ACCEPTED).entity(apiResponse).build();

        } catch (Exception e) {
            Log.error("[CLAIM-API] Error submitting claim: " + e.getMessage(), e);
            throw new ClaimProcessingException(claimSubmission.getClaimNumber(), "Failed to submit claim", "CLAIM_SUBMISSION_FAILED");
        }
    }

    @POST
    @Path("/urgent")
    public Response submitUrgentClaim(@Valid ClaimSubmission claimSubmission) {
        Log.info("[CLAIM-API] Submitting urgent claim for policy: " + claimSubmission.getPolicyNumber());

        try {
            claimSubmission.setPriority(com.example.insurance.entity.Claim.ClaimPriority.URGENT);

            kafkaProducerService.publishHighPriorityClaim(claimSubmission);

            ClaimSubmissionResponse response = ClaimSubmissionResponse.urgentAccepted(
                claimSubmission.getClaimNumber(),
                claimSubmission.getPolicyNumber()
            );

            ApiResponse<ClaimSubmissionResponse> apiResponse = ApiResponse.success(
                "Urgent claim submitted successfully for expedited processing",
                response
            );

            return Response.status(Response.Status.ACCEPTED).entity(apiResponse).build();

        } catch (Exception e) {
            Log.error("[CLAIM-API] Error submitting urgent claim: " + e.getMessage(), e);
            throw new ClaimProcessingException(claimSubmission.getClaimNumber(), "Failed to submit urgent claim", "URGENT_CLAIM_SUBMISSION_FAILED");
        }
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSystemStatus() {
        return Response.ok("Insurance Claim Processing System - Kafka Integration Active").build();
    }
}