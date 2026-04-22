package com.smartcampus.mapper;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps SensorUnavailableException to HTTP 403 Forbidden.
 *
 * Triggered when a client attempts to post a measurement to a sensor whose
 * operational status is MAINTENANCE or OFFLINE. The hardware is physically
 * disconnected in these states and cannot produce valid readings. HTTP 403
 * communicates that the operation is understood but refused by the server
 * due to the current device state — the client must wait for the sensor to
 * return to ACTIVE status before attempting to submit data.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        ErrorResponse payload = new ErrorResponse(
                403,
                "Forbidden",
                ex.getMessage()
        );

        return Response.status(Response.Status.FORBIDDEN)
                .entity(payload)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
