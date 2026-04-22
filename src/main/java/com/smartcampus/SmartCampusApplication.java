package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application subclass that declares the versioned API base path.
 *
 * The @ApplicationPath annotation establishes "/api/v1" as the root API path
 * within the deployed web application context.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // JAX-RS auto-discovers all @Path and @Provider classes
    // within the scanned package hierarchy.
}
