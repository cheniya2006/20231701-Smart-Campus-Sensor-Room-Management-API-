package com.smartcampus.mapper;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.RoomNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps RoomNotEmptyException to HTTP 409 Conflict.
 *
 * Triggered when a client attempts to delete a campus room that still has
 * sensor devices assigned to it. The 409 status communicates that the
 * request is valid in isolation, but conflicts with the current state of the
 * resource — the room cannot be removed until its sensors are cleared.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        ErrorResponse payload = new ErrorResponse(
                409,
                "Conflict",
                ex.getMessage()
        );

        return Response.status(Response.Status.CONFLICT)
                .entity(payload)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
