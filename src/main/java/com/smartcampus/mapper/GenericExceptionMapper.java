package com.smartcampus.mapper;

import com.smartcampus.exception.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global safety-net exception mapper that intercepts any unhandled
 * Throwable and converts it to a clean HTTP 500 Internal Server Error.
 *
 * This mapper deliberately avoids catching WebApplicationException subclasses
 * (e.g. NotFoundException, NotAllowedException, NotSupportedException) because
 * JAX-RS already assigns those meaningful 4xx status codes. Re-mapping them to
 * 500 would hide legitimate client errors and break standard REST semantics.
 *
 * Security rationale for suppressing stack traces:
 *   - Java stack traces reveal internal package structures and class names,
 *     giving attackers a map of the application's architecture.
 *   - Framework version strings (e.g. Jersey 2.41, Grizzly 4.x) allow
 *     attackers to search for published CVEs targeting those exact versions.
 *   - File system paths embedded in traces can expose server configuration.
 *   - Business logic flow visible in trace sequences may reveal
 *     unprotected branches or potential injection points.
 *
 * By returning only a generic message here, all sensitive diagnostic
 * information is kept server-side in the application log.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger API_LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable caughtError) {

        // Let JAX-RS handle its own WebApplicationExceptions (404, 405, 415, etc.)
        // with their correct status codes rather than masking them as 500.
        if (caughtError instanceof WebApplicationException webEx) {
            return webEx.getResponse();
        }

        // Log the full stack trace internally — never expose it to the client
        API_LOGGER.log(Level.SEVERE,
                "Unhandled runtime exception intercepted by global safety-net mapper.", caughtError);

        ErrorResponse safeResponse = new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected condition was encountered. The incident has been logged. " +
                "Please contact the Smart Campus support team if this problem persists."
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(safeResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
