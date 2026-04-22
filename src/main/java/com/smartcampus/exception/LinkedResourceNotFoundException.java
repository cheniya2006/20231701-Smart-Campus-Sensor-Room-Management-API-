package com.smartcampus.exception;

/**
 * Thrown when a request body references a linked entity (e.g. roomId) that
 * does not exist in the registry.
 *
 * HTTP 422 Unprocessable Entity is more semantically accurate than 404 here
 * because: the request URL itself is valid; the JSON payload is syntactically
 * correct; the problem is a semantic violation — a field inside the body
 * references an entity that cannot be resolved. Returning 404 would imply the
 * endpoint does not exist, which is misleading to the client.
 *
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    /**
     * @param detail human-readable explanation identifying the missing reference
     */
    public LinkedResourceNotFoundException(String detail) {
        super(detail);
    }
}
