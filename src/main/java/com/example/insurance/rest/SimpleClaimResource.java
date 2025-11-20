package com.example.insurance.rest;

import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.entity.Claim;
import com.example.insurance.service.SimpleClaimService;
import com.example.insurance.util.SampleDataGenerator;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@Path("/api/claims")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimpleClaimResource {

    @Inject
    SimpleClaimService claimService;

    @POST
    @Path("/submit")
    public Response submitClaim(@Valid ClaimSubmission claimSubmission) {
        try {
            Claim claim = claimService.submitClaim(claimSubmission);
            Log.info("Claim submitted successfully: " + claim.getClaimNumber());

            return Response.status(Response.Status.CREATED)
                    .entity(claim)
                    .build();

        } catch (Exception e) {
            Log.error("Error submitting claim: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to submit claim: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{claimNumber}")
    public Response getClaim(@PathParam("claimNumber") String claimNumber) {
        try {
            Optional<Claim> claim = claimService.getClaimByNumber(claimNumber);

            if (claim.isPresent()) {
                return Response.ok(claim.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Claim not found\"}")
                        .build();
            }

        } catch (Exception e) {
            Log.error("Error retrieving claim: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to retrieve claim\"}")
                    .build();
        }
    }

    @GET
    @Path("/")
    public Response getAllClaims() {
        try {
            List<Claim> claims = claimService.getAllClaims();
            return Response.ok(claims).build();

        } catch (Exception e) {
            Log.error("Error retrieving claims: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to retrieve claims\"}")
                    .build();
        }
    }

    @GET
    @Path("/pending")
    public Response getPendingClaims() {
        try {
            List<Claim> pendingClaims = claimService.getPendingClaims();
            return Response.ok(pendingClaims).build();

        } catch (Exception e) {
            Log.error("Error retrieving pending claims: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to retrieve pending claims\"}")
                    .build();
        }
    }

    @POST
    @Path("/sample")
    public Response createSampleClaim() {
        try {
            ClaimSubmission sampleClaim = SampleDataGenerator.generateSampleClaimSubmission();
            Claim claim = claimService.submitClaim(sampleClaim);
            Log.info("Sample claim created: " + claim.getClaimNumber());

            return Response.status(Response.Status.CREATED)
                    .entity(claim)
                    .build();

        } catch (Exception e) {
            Log.error("Error creating sample claim: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to create sample claim\"}")
                    .build();
        }
    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response health() {
        return Response.ok("Simple Claim service is healthy").build();
    }
}