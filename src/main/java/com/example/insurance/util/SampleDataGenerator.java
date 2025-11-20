package com.example.insurance.util;

import com.example.insurance.dto.ClaimSubmission;
import com.example.insurance.entity.Claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

public class SampleDataGenerator {

    private static final Random random = new Random();

    public static ClaimSubmission generateSampleClaimSubmission() {
        String[] policyNumbers = {"POL001", "POL002", "POL003", "POL004"};
        String[] policyholderIds = {"PH001", "PH002", "PH003", "PH004"};
        String[] policyholderNames = {"John Doe", "Jane Smith", "Bob Johnson", "Alice Brown"};
        String[] policyholderEmails = {"john.doe@email.com", "jane.smith@email.com", "bob.johnson@email.com", "alice.brown@email.com"};

        Claim.ClaimType[] claimTypes = Claim.ClaimType.values();
        Claim.ClaimPriority[] priorities = Claim.ClaimPriority.values();

        String policyNumber = policyNumbers[random.nextInt(policyNumbers.length)];
        String policyholderId = policyholderIds[random.nextInt(policyholderIds.length)];
        String policyholderName = policyholderNames[random.nextInt(policyholderNames.length)];
        String policyholderEmail = policyholderEmails[random.nextInt(policyholderEmails.length)];

        BigDecimal claimedAmount = BigDecimal.valueOf(100 + random.nextDouble() * 15000);

        // Generate incident date between 1 and 60 days ago
        LocalDate incidentDate = LocalDate.now().minusDays(1 + random.nextInt(60));

        ClaimSubmission claim = new ClaimSubmission();
        claim.setClaimNumber("CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        claim.setPolicyNumber(policyNumber);
        claim.setClaimType(claimTypes[random.nextInt(claimTypes.length)]);
        claim.setIncidentDate(incidentDate);
        claim.setClaimedAmount(claimedAmount);
        claim.setDescription(generateClaimDescription());
        claim.setPriority(priorities[random.nextInt(priorities.length)]);
        claim.setPolicyholderId(policyholderId);
        claim.setPolicyholderName(policyholderName);
        claim.setPolicyholderEmail(policyholderEmail);

        return claim;
    }

    private static String generateClaimDescription() {
        String[] descriptions = {
                "Car accident on highway during morning commute",
                "Medical expenses for emergency room visit",
                "Property damage due to severe storm",
                "Theft of personal belongings from vehicle",
                "Travel cancellation due to medical emergency",
                "Disability claim following workplace injury",
                "Natural disaster damage to home",
                "Lost baggage during international flight"
        };

        return descriptions[random.nextInt(descriptions.length)];
    }

    public static ClaimSubmission generateHighRiskClaim() {
        ClaimSubmission claim = generateSampleClaimSubmission();

        // Modify to create high-risk characteristics
        claim.setClaimedAmount(BigDecimal.valueOf(20000 + random.nextDouble() * 30000));
        claim.setIncidentDate(LocalDate.now().minusDays(45 + random.nextInt(30)));
        claim.setClaimType(Claim.ClaimType.THEFT);
        claim.setPriority(Claim.ClaimPriority.URGENT);

        return claim;
    }

    public static ClaimSubmission generateFraudulentClaim() {
        ClaimSubmission claim = generateSampleClaimSubmission();

        // Characteristics that might indicate fraud
        claim.setClaimedAmount(BigDecimal.valueOf(50000 + random.nextDouble() * 50000));
        claim.setIncidentDate(LocalDate.now().minusDays(90 + random.nextInt(30)));
        claim.setClaimType(Claim.ClaimType.OTHER);
        claim.setDescription("Vague description with limited details");
        claim.setPriority(Claim.ClaimPriority.URGENT);

        return claim;
    }
}