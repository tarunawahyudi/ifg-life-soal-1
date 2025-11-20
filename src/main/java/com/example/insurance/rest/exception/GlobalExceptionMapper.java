package com.example.insurance.rest.exception;

import com.example.insurance.dto.ApiResponse;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        Log.error("Unexpected error occurred: " + exception.getMessage(), exception);

        ApiResponse<Object> errorResponse = ApiResponse.error(
            "An internal error occurred. Please try again later.",
            "INTERNAL_SERVER_ERROR"
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}