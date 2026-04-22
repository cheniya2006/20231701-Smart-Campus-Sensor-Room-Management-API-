package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point — bootstraps the embedded Grizzly HTTP server
 * and registers all JAX-RS components via package scanning.
 *
 * Note on @ApplicationPath: when running with an embedded Grizzly container,
 * the @ApplicationPath annotation defined on SmartCampusApplication is not
 * honoured automatically. The versioned base URI (/api/v1/) is therefore
 * specified directly in BASE_URI so the path is applied consistently.
 */
public class Main {

    /** Versioned base URI served by the embedded Grizzly container. */
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    private static final Logger SERVER_LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * Assembles and starts the Grizzly HTTP server with the Jersey resource
     * configuration. Package scanning registers all @Path, @Provider,
     * and @Provider-annotated classes automatically.
     *
     * @return the running HttpServer instance
     */
    public static HttpServer startServer() {
        final ResourceConfig jerseyConfig = new ResourceConfig()
                .packages("com.smartcampus")
                .register(JacksonFeature.class);

        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), jerseyConfig);
    }

    /**
     * Main entry point. Starts the server and blocks until Enter is pressed.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();

        SERVER_LOGGER.log(Level.INFO, "=============================================================");
        SERVER_LOGGER.log(Level.INFO, "  Smart Campus Sensor Management API");
        SERVER_LOGGER.log(Level.INFO, "  Server running at : http://localhost:8080/");
        SERVER_LOGGER.log(Level.INFO, "  API entry point   : {0}", BASE_URI);
        SERVER_LOGGER.log(Level.INFO, "  Discovery endpoint: {0}", BASE_URI);
        SERVER_LOGGER.log(Level.INFO, "=============================================================");
        SERVER_LOGGER.log(Level.INFO, "  Press <Enter> to gracefully shut down the server.");

        System.in.read();
        server.shutdownNow();
        SERVER_LOGGER.log(Level.INFO, "Server stopped. Goodbye.");
    }
}
