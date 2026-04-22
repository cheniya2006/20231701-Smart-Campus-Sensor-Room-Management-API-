package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application subclass that declares the versioned API base path.
 *
 * The @ApplicationPath annotation establishes "/api/v1" as the root context
 * for all resource endpoints. In a servlet-container deployment, this
 * annotation is honoured automatically. When running with the embedded Grizzly
 * server (as in this project), the equivalent path is set in Main.BASE_URI.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // JAX-RS auto-discovers all @Path and @Provider classes
    // within the scanned package hierarchy.
}
