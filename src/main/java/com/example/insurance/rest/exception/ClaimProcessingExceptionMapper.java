package com.example.insurance.rest.exception;

import com.example.insurance.dto.ApiResponse;
import com.example.insurance.exception.ClaimProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ClaimProcessingExceptionMapper implements ExceptionMapper<ClaimProcessingException> {

    @Override
    public Response toResponse(ClaimProcessingException exception) {
        ApiResponse<Object> errorResponse = ApiResponse.error(
            exception.getMessage(),
            exception.getErrorCode()
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }
}