package com.smartcampus.mapper;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * Used when a client submits a syntactically valid JSON payload whose content
 * cannot be processed because a referenced entity (e.g. roomId) does not exist.
 *
 * HTTP 422 is semantically superior to 404 in this context:
 *   - 404 implies the requested URL/endpoint was not found, which is incorrect
 *     because the endpoint itself (e.g. POST /api/v1/sensors) exists and resolved.
 *   - 422 precisely communicates that the server understood and received the
 *     request but cannot process the payload due to a semantic validation failure
 *     — a referenced linked resource is absent from the system.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorResponse payload = new ErrorResponse(
                422,
                "Unprocessable Entity",
                ex.getMessage()
        );

        return Response.status(422)
                .entity(payload)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
