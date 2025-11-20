package com.example.insurance.rest;

import com.example.insurance.dto.ClaimSubmission;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ClaimSubmissionResourceTest {

    private ClaimSubmission validClaimSubmission;
    private ClaimSubmission urgentClaimSubmission;

    @BeforeEach
    void setUp() {
        validClaimSubmission = new ClaimSubmission();
        validClaimSubmission.setClaimNumber("TEST-REST-001");
        validClaimSubmission.setPolicyNumber("POL-REST-001");
        validClaimSubmission.setClaimType(com.example.insurance.entity.Claim.ClaimType.ACCIDENT);
        validClaimSubmission.setIncidentDate(LocalDate.of(2024, 1, 15));
        validClaimSubmission.setClaimedAmount(new BigDecimal("5000.00"));
        validClaimSubmission.setDescription("Car accident claim");
        validClaimSubmission.setPriority(com.example.insurance.entity.Claim.ClaimPriority.NORMAL);
        validClaimSubmission.setPolicyholderId("PH-REST-001");
        validClaimSubmission.setPolicyholderName("John Doe");
        validClaimSubmission.setPolicyholderEmail("john.doe@email.com");

        urgentClaimSubmission = new ClaimSubmission();
        urgentClaimSubmission.setClaimNumber("TEST-URGENT-001");
        urgentClaimSubmission.setPolicyNumber("POL-URGENT-001");
        urgentClaimSubmission.setClaimType(com.example.insurance.entity.Claim.ClaimType.ILLNESS);
        urgentClaimSubmission.setIncidentDate(LocalDate.of(2024, 1, 16));
        urgentClaimSubmission.setClaimedAmount(new BigDecimal("8000.00"));
        urgentClaimSubmission.setDescription("Medical emergency claim");
        urgentClaimSubmission.setPriority(com.example.insurance.entity.Claim.ClaimPriority.NORMAL); // Will be set to URGENT by endpoint
        urgentClaimSubmission.setPolicyholderId("PH-URGENT-001");
        urgentClaimSubmission.setPolicyholderName("Jane Smith");
        urgentClaimSubmission.setPolicyholderEmail("jane.smith@email.com");
    }

    @Test
    public void testSubmitClaim_Success() {
        given()
            .contentType(ContentType.JSON)
            .body(validClaimSubmission)
        .when()
            .post("/api/claims/submit")
        .then()
            .statusCode(202) // ACCEPTED
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("message", equalTo("Claim submitted successfully for processing"))
            .body("data.claimNumber", equalTo("TEST-REST-001"))
            .body("data.policyNumber", equalTo("POL-REST-001"))
            .body("data.status", equalTo("ACCEPTED"))
            .body("data.message", equalTo("Claim submitted successfully for processing"))
            .body("timestamp", notNullValue());
    }

    @Test
    public void testSubmitUrgentClaim_Success() {
        given()
            .contentType(ContentType.JSON)
            .body(urgentClaimSubmission)
        .when()
            .post("/api/claims/urgent")
        .then()
            .statusCode(202) // ACCEPTED
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("message", equalTo("Urgent claim submitted successfully for expedited processing"))
            .body("data.claimNumber", equalTo("TEST-URGENT-001"))
            .body("data.policyNumber", equalTo("POL-URGENT-001"))
            .body("data.status", equalTo("URGENT_ACCEPTED"))
            .body("data.message", equalTo("Urgent claim submitted successfully for expedited processing"))
            .body("timestamp", notNullValue());
    }

    @Test
    public void testSubmitClaim_InvalidRequest_MissingRequiredFields() {
        ClaimSubmission invalidClaim = new ClaimSubmission();
        // Missing most required fields

        given()
            .contentType(ContentType.JSON)
            .body(invalidClaim)
        .when()
            .post("/api/claims/submit")
        .then()
            .statusCode(400); // Bad Request for validation errors
    }

    @Test
    public void testSubmitClaim_InvalidRequest_NegativeAmount() {
        ClaimSubmission invalidClaim = new ClaimSubmission();
        invalidClaim.setClaimNumber("TEST-INVALID-001");
        invalidClaim.setPolicyNumber("POL-INVALID-001");
        invalidClaim.setClaimType(com.example.insurance.entity.Claim.ClaimType.ACCIDENT);
        invalidClaim.setIncidentDate(LocalDate.of(2024, 1, 15));
        invalidClaim.setClaimedAmount(new BigDecimal("-1000.00")); // Negative amount
        invalidClaim.setDescription("Invalid amount claim");
        invalidClaim.setPriority(com.example.insurance.entity.Claim.ClaimPriority.NORMAL);
        invalidClaim.setPolicyholderId("PH-INVALID-001");
        invalidClaim.setPolicyholderName("Invalid User");
        invalidClaim.setPolicyholderEmail("invalid@email.com");

        given()
            .contentType(ContentType.JSON)
            .body(invalidClaim)
        .when()
            .post("/api/claims/submit")
        .then()
            .statusCode(400); // Bad Request for validation errors
    }

    @Test
    public void testSubmitClaim_InvalidRequest_EmptyPolicyNumber() {
        ClaimSubmission invalidClaim = new ClaimSubmission();
        invalidClaim.setClaimNumber("TEST-INVALID-002");
        invalidClaim.setPolicyNumber(""); // Empty policy number
        invalidClaim.setClaimType(com.example.insurance.entity.Claim.ClaimType.ACCIDENT);
        invalidClaim.setIncidentDate(LocalDate.of(2024, 1, 15));
        invalidClaim.setClaimedAmount(new BigDecimal("5000.00"));
        invalidClaim.setDescription("Empty policy number claim");
        invalidClaim.setPriority(com.example.insurance.entity.Claim.ClaimPriority.NORMAL);
        invalidClaim.setPolicyholderId("PH-INVALID-002");
        invalidClaim.setPolicyholderName("Invalid User");
        invalidClaim.setPolicyholderEmail("invalid@email.com");

        given()
            .contentType(ContentType.JSON)
            .body(invalidClaim)
        .when()
            .post("/api/claims/submit")
        .then()
            .statusCode(400); // Bad Request for validation errors
    }

    @Test
    public void testGetSystemStatus_Success() {
        given()
        .when()
            .get("/api/claims/status")
        .then()
            .statusCode(200)
            .contentType("text/plain")
            .body(equalTo("Insurance Claim Processing System - Kafka Integration Active"));
    }

    @Test
    public void testSubmitClaim_InvalidEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .body(validClaimSubmission)
        .when()
            .post("/api/claims/invalid-endpoint")
        .then()
            .statusCode(404); // Not Found
    }

    @Test
    public void testSubmitClaim_InvalidMethod() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/claims/submit") // GET instead of POST
        .then()
            .statusCode(405); // Method Not Allowed
    }

    @Test
    public void testSubmitClaim_InvalidContentType() {
        given()
            .contentType("text/plain") // Wrong content type
            .body(validClaimSubmission)
        .when()
            .post("/api/claims/submit")
        .then()
            .statusCode(415); // Unsupported Media Type
    }

    @Test
    public void testSubmitUrgentClaim_ValidationOfPriorityOverride() {
        // Set initial priority to NORMAL
        urgentClaimSubmission.setPriority(com.example.insurance.entity.Claim.ClaimPriority.NORMAL);

        given()
            .contentType(ContentType.JSON)
            .body(urgentClaimSubmission)
        .when()
            .post("/api/claims/urgent")
        .then()
            .statusCode(202)
            .body("success", equalTo(true))
            .body("data.status", equalTo("URGENT_ACCEPTED"));
            // The endpoint should override priority to URGENT regardless of input
    }

    @Test
    public void testSubmitClaim_FutureIncidentDate() {
        ClaimSubmission futureDateClaim = new ClaimSubmission();
        futureDateClaim.setClaimNumber("TEST-FUTURE-001");
        futureDateClaim.setPolicyNumber("POL-FUTURE-001");
        futureDateClaim.setClaimType(com.example.insurance.entity.Claim.ClaimType.ACCIDENT);
        futureDateClaim.setIncidentDate(LocalDate.now().plusDays(10)); // Future date
        futureDateClaim.setClaimedAmount(new BigDecimal("5000.00"));
        futureDateClaim.setDescription("Future incident claim");
        futureDateClaim.setPriority(com.example.insurance.entity.Claim.ClaimPriority.NORMAL);
        futureDateClaim.setPolicyholderId("PH-FUTURE-001");
        futureDateClaim.setPolicyholderName("Future User");
        futureDateClaim.setPolicyholderEmail("future@email.com");

        given()
            .contentType(ContentType.JSON)
            .body(futureDateClaim)
        .when()
            .post("/api/claims/submit")
        .then()
            .statusCode(400); // Bad Request for validation errors
    }
}