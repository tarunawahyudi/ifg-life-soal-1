package com.example.insurance.health;

import io.quarkus.logging.Log;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class InsuranceHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        try {
            return HealthCheckResponse.builder()
                    .name("Insurance Claim Processor")
                    .withData("status", "Ready")
                    .withData("version", "1.0.0")
                    .up()
                    .build();

        } catch (Exception e) {
            Log.error("Health check failed: " + e.getMessage(), e);
            return HealthCheckResponse.builder()
                    .name("Insurance Claim Processor")
                    .withData("error", e.getMessage())
                    .down()
                    .build();
        }
    }
}