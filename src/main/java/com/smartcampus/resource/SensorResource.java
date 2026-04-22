package com.smartcampus.resource;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorRoom;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS resource class responsible for sensor device management.
 *
 * Handles CRUD operations for the /api/v1/sensors collection and exposes a
 * sub-resource locator that delegates reading history requests to
 * SensorReadingResource.
 *
 * Query parameter filtering:
 * GET /api/v1/sensors?type=CO2  — the @QueryParam approach is preferable to
 * embedding the filter in the path (e.g. /sensors/type/CO2) because:
 *   - Query parameters are inherently optional, preserving backward compatibility.
 *   - Multiple filters can be combined without altering the URL structure.
 *   - Path segments should identify resources, not describe filter criteria.
 *   - It aligns with established REST conventions for collection filtering.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    /** Shared singleton data registry injected on each request. */
    private final DataStore campusRegistry = DataStore.getInstance();

    // -------------------------------------------------------------------------
    // GET /api/v1/sensors  (with optional ?type= filter)
    // -------------------------------------------------------------------------

    /**
     * Returns all registered sensor devices, optionally filtered by type.
     *
     * @param typeFilter optional query parameter; when present, only sensors
     *                   whose type matches (case-insensitive) are returned
     */
    @GET
    public Response listSensors(@QueryParam("type") String typeFilter) {
        List<Sensor> deviceList;

        if (typeFilter != null && !typeFilter.isBlank()) {
            deviceList = campusRegistry.getSensorsByType(typeFilter);
        } else {
            deviceList = campusRegistry.getAllSensors();
        }

        return Response.ok(deviceList).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}
    // -------------------------------------------------------------------------

    /**
     * Retrieves full metadata for a single sensor device.
     *
     * @param sensorId path parameter containing the sensor UUID
     */
    @GET
    @Path("/{sensorId}")
    public Response fetchSensorById(@PathParam("sensorId") String sensorId) {
        Sensor targetDevice = campusRegistry.getSensorById(sensorId);

        if (targetDevice == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "No sensor was found with ID '" + sensorId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        return Response.ok(targetDevice).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/sensors
    // -------------------------------------------------------------------------

    /**
     * Registers a new sensor device in the campus registry.
     *
     * Referential integrity check: the roomId supplied in the request body
     * must correspond to an existing campus room. If it does not, a
     * LinkedResourceNotFoundException is thrown and mapped to HTTP 422 by
     * LinkedResourceNotFoundExceptionMapper.
     *
     * Content negotiation: the @Consumes(APPLICATION_JSON) annotation means
     * that if a client sends a request with Content-Type: text/plain or
     * application/xml, JAX-RS will immediately return HTTP 415 Unsupported
     * Media Type without invoking this method at all.
     *
     * @param uriInfo injected context for building the Location URI
     * @param payload sensor properties supplied by the client
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerSensor(@Context UriInfo uriInfo, Sensor payload) {

        // Validate that a roomId was provided
        if (payload.getRoomId() == null || payload.getRoomId().isBlank()) {
            throw new LinkedResourceNotFoundException(
                    "The 'roomId' field is required when registering a sensor."
            );
        }

        // Validate that the referenced room actually exists
        if (!campusRegistry.roomExists(payload.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: the roomId '" + payload.getRoomId() +
                    "' does not correspond to any known campus room. " +
                    "Please provide a valid room ID."
            );
        }

        // Build a server-managed sensor object with a generated UUID
        Sensor newDevice = new Sensor(payload.getRoomId(), payload.getType(), payload.getName());
        if (payload.getStatus() != null && !payload.getStatus().isBlank()) {
            newDevice.setStatus(payload.getStatus());
        }
        campusRegistry.addSensor(newDevice);

        URI locationUri = uriInfo.getAbsolutePathBuilder()
                .path(newDevice.getId())
                .build();

        return Response.created(locationUri).entity(newDevice).build();
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/sensors/{sensorId}
    // -------------------------------------------------------------------------

    /**
     * Updates the mutable properties of an existing sensor.
     *
     * When the roomId is changed, both the old and new parent rooms'
     * sensorIds lists are updated to keep referential consistency intact.
     *
     * @param sensorId    path parameter containing the sensor UUID
     * @param updatedData replacement values supplied by the client
     */
    @PUT
    @Path("/{sensorId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updatedData) {
        Sensor existingDevice = campusRegistry.getSensorById(sensorId);

        if (existingDevice == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "Cannot update: no sensor was found with ID '" + sensorId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        // Apply selective field updates
        if (updatedData.getName()   != null) existingDevice.setName(updatedData.getName());
        if (updatedData.getType()   != null) existingDevice.setType(updatedData.getType());
        if (updatedData.getStatus() != null) existingDevice.setStatus(updatedData.getStatus());

        // Handle room reassignment: update both the old and new parent rooms' lists
        if (updatedData.getRoomId() != null && !updatedData.getRoomId().equals(existingDevice.getRoomId())) {
            if (!campusRegistry.roomExists(updatedData.getRoomId())) {
                throw new LinkedResourceNotFoundException(
                        "Cannot reassign sensor: roomId '" + updatedData.getRoomId() +
                        "' does not exist in the system."
                );
            }
            // Detach from old room
            SensorRoom oldRoom = campusRegistry.getRoomById(existingDevice.getRoomId());
            if (oldRoom != null) {
                oldRoom.removeSensorId(sensorId);
            }
            // Attach to new room
            SensorRoom newRoom = campusRegistry.getRoomById(updatedData.getRoomId());
            if (newRoom != null) {
                newRoom.addSensorId(sensorId);
            }
            existingDevice.setRoomId(updatedData.getRoomId());
        }

        return Response.ok(existingDevice).build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/sensors/{sensorId}
    // -------------------------------------------------------------------------

    /**
     * Removes a sensor and its associated reading history from the registry.
     *
     * @param sensorId path parameter containing the sensor UUID
     */
    @DELETE
    @Path("/{sensorId}")
    public Response removeSensor(@PathParam("sensorId") String sensorId) {
        Sensor targetDevice = campusRegistry.getSensorById(sensorId);

        if (targetDevice == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "Cannot delete: no sensor was found with ID '" + sensorId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        campusRegistry.removeSensor(sensorId);

        Map<String, Object> confirmation = new LinkedHashMap<>();
        confirmation.put("message", "Sensor successfully removed from the registry.");
        confirmation.put("deletedSensorId", sensorId);
        confirmation.put("deletedSensorName", targetDevice.getName());

        return Response.ok(confirmation).build();
    }

    // -------------------------------------------------------------------------
    // Sub-Resource Locator  — /api/v1/sensors/{sensorId}/readings
    // -------------------------------------------------------------------------

    /**
     * Sub-resource locator that delegates all requests targeting the readings
     * sub-collection to SensorReadingResource.
     *
     * The locator pattern keeps this class focused on sensor-level operations
     * while reading history logic is encapsulated in its own dedicated class.
     * This separation reduces class size, improves testability, and allows
     * independent evolution of each resource level.
     *
     * @param sensorId path parameter extracted from the URI
     * @return a new SensorReadingResource scoped to the given sensorId
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource locateReadingsSubResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
