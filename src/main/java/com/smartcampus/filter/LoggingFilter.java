package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS filter providing API-wide request and response observability.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so that
 * a single registered component covers the full request lifecycle.
 *
 * Why JAX-RS filters are superior to inline Logger.info() calls:
 *   - Cross-cutting concerns such as logging, authentication, and metrics
 *     belong outside business logic — filters enforce this separation cleanly.
 *   - A single filter automatically covers every endpoint, including ones
 *     added in the future, eliminating the risk of forgotten logging.
 *   - Changing the log format or switching logging frameworks requires
 *     editing one file rather than dozens of resource methods.
 *   - Filters can be toggled, reordered, or replaced without touching any
 *     resource class — ideal for feature flags or environment-specific config.
 *   - Resource methods remain readable and focused on domain logic alone.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger API_LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Intercepts every incoming HTTP request before it reaches a resource method.
     * Records the HTTP verb and the full request URI for audit and debugging.
     */
    @Override
    public void filter(ContainerRequestContext inboundRequest) throws IOException {
        String httpVerb   = inboundRequest.getMethod();
        String requestUri = inboundRequest.getUriInfo().getRequestUri().toString();

        API_LOGGER.info("[INBOUND]  " + httpVerb + " " + requestUri);
    }

    /**
     * Intercepts every outgoing HTTP response after the resource method returns.
     * Records the HTTP verb, URI, and final status code for traceability.
     */
    @Override
    public void filter(ContainerRequestContext inboundRequest,
                       ContainerResponseContext outboundResponse) throws IOException {

        String httpVerb   = inboundRequest.getMethod();
        String requestUri = inboundRequest.getUriInfo().getRequestUri().toString();
        int    statusCode = outboundResponse.getStatus();

        API_LOGGER.info("[OUTBOUND] " + httpVerb + " " + requestUri + " => HTTP " + statusCode);
    }
}
