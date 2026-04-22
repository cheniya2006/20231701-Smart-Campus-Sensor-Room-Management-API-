package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root discovery endpoint for the Smart Campus Sensor Management API.
 *
 * Responds to GET /api/v1 with a JSON document containing essential API
 * metadata and navigable hypermedia links following HATEOAS conventions.
 *
 * HATEOAS (Hypermedia as the Engine of Application State) is a hallmark of
 * mature RESTful APIs. By embedding navigation links directly in responses,
 * the server becomes self-documenting: a client that discovers this endpoint
 * can explore the entire API without consulting external documentation.
 * This decouples clients from hard-coded URL knowledge, allows the server
 * to evolve URL structures while updating links dynamically, and dramatically
 * reduces integration friction for new developers or automated tooling.
 */
@Path("/")
public class DiscoveryResource {

    /**
     * GET /api/v1
     * Returns a JSON metadata document with versioning, contact, and resource links.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response describeApi() {
        Map<String, Object> apiManifest = new LinkedHashMap<>();

        // Core API identity
        apiManifest.put("apiName",      "Smart Campus Sensor Management API");
        apiManifest.put("version",      "1.0");
        apiManifest.put("description",  "RESTful service for managing campus IoT infrastructure — " +
                                        "rooms, sensor devices, and live/historical measurement data.");
        apiManifest.put("specVersion",  "JAX-RS 2.1 / Jersey 2.41");

        // Administrative contact details
        Map<String, String> adminContact = new LinkedHashMap<>();
        adminContact.put("team",       "Campus Infrastructure & IoT Engineering");
        adminContact.put("email",      "campus-iot@university.ac.uk");
        adminContact.put("department", "Facilities Management & Digital Systems");
        apiManifest.put("contact", adminContact);

        // HATEOAS-style resource index — clients navigate from here
        Map<String, Object> resourceIndex = new LinkedHashMap<>();

        Map<String, String> roomsEntry = new LinkedHashMap<>();
        roomsEntry.put("href",        "/api/v1/rooms");
        roomsEntry.put("methods",     "GET, POST, PUT, DELETE");
        roomsEntry.put("description", "Campus room registry — list, create, update, and remove rooms");
        resourceIndex.put("rooms", roomsEntry);

        Map<String, String> sensorsEntry = new LinkedHashMap<>();
        sensorsEntry.put("href",        "/api/v1/sensors");
        sensorsEntry.put("methods",     "GET, POST, PUT, DELETE");
        sensorsEntry.put("description", "Sensor device registry — manage IoT devices and filter by type");
        resourceIndex.put("sensors", sensorsEntry);

        Map<String, String> readingsEntry = new LinkedHashMap<>();
        readingsEntry.put("href",        "/api/v1/sensors/{sensorId}/readings");
        readingsEntry.put("methods",     "GET, POST");
        readingsEntry.put("description", "Sensor reading history — append measurements and retrieve logs");
        resourceIndex.put("readings", readingsEntry);

        apiManifest.put("resources", resourceIndex);

        // Runtime server information
        Map<String, String> runtimeInfo = new LinkedHashMap<>();
        runtimeInfo.put("baseUri",  "http://localhost:8080/api/v1");
        runtimeInfo.put("protocol", "HTTP/1.1");
        runtimeInfo.put("status",   "operational");
        apiManifest.put("server", runtimeInfo);

        return Response.ok(apiManifest).build();
    }
}
