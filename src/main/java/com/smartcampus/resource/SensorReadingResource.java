package com.smartcampus.resource;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Sub-resource class that manages the historical reading log for a single sensor.
 *
 * This class is deliberately not annotated with @Path because it is never
 * registered directly with the JAX-RS runtime. Instead, it is instantiated
 * exclusively by the sub-resource locator method in SensorResource, which
 * handles the path matching for /api/v1/sensors/{sensorId}/readings.
 *
 * Sub-Resource Locator architectural benefits:
 *   - Keeps SensorResource focused on sensor-level CRUD without growing into
 *     a large monolithic controller.
 *   - Each class has a single, well-defined responsibility (SRP).
 *   - Reading-specific business logic (e.g. maintenance checks, side effects)
 *     can be evolved and tested independently of sensor management code.
 *   - JAX-RS dynamically dispatches to this class at runtime, enabling clean
 *     hierarchical URL structures without manual path concatenation.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    /** UUID of the sensor that owns this reading sub-resource. */
    private final String ownerSensorId;

    /** Shared singleton data registry. */
    private final DataStore campusRegistry = DataStore.getInstance();

    /**
     * Constructs a reading resource scoped to the specified sensor.
     * Called exclusively by SensorResource's sub-resource locator.
     *
     * @param ownerSensorId UUID of the parent sensor
     */
    public SensorReadingResource(String ownerSensorId) {
        this.ownerSensorId = ownerSensorId;
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings
    // -------------------------------------------------------------------------

    /**
     * Retrieves the complete measurement history for the parent sensor.
     *
     * Returns an empty array rather than 404 when no readings have been
     * recorded yet, following the principle of least surprise for list endpoints.
     */
    @GET
    public Response fetchReadingHistory() {
        Sensor parentSensor = campusRegistry.getSensorById(ownerSensorId);

        if (parentSensor == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "No sensor was found with ID '" + ownerSensorId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        List<SensorReading> history = campusRegistry.getReadingsBySensorId(ownerSensorId);
        return Response.ok(history).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings/{readingId}
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single measurement event by its unique reading ID.
     *
     * @param readingId path parameter containing the reading UUID
     */
    @GET
    @Path("/{readingId}")
    public Response fetchReadingById(@PathParam("readingId") String readingId) {
        Sensor parentSensor = campusRegistry.getSensorById(ownerSensorId);

        if (parentSensor == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "No sensor was found with ID '" + ownerSensorId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        List<SensorReading> history = campusRegistry.getReadingsBySensorId(ownerSensorId);
        for (SensorReading entry : history) {
            if (entry.getId().equals(readingId)) {
                return Response.ok(entry).build();
            }
        }

        ErrorResponse notFound = new ErrorResponse(
                404, "Not Found",
                "No reading with ID '" + readingId + "' was found for sensor '" + ownerSensorId + "'."
        );
        return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings
    // -------------------------------------------------------------------------

    /**
     * Records a new measurement from the parent sensor.
     *
     * Business rules enforced before recording:
     *   - Sensor must exist; otherwise HTTP 404 is returned.
     *   - Sensor must not be in MAINTENANCE or OFFLINE status; those states
     *     indicate the hardware is physically disconnected and cannot produce
     *     valid readings. A SensorUnavailableException is thrown, which the
     *     SensorUnavailableExceptionMapper converts to HTTP 403 Forbidden.
     *
     * Side effect: on successful recording, the parent sensor's currentValue
     * field is updated to reflect the latest measurement, keeping data
     * consistent across the /sensors and /sensors/{id}/readings endpoints.
     *
     * @param payload reading data supplied by the client (value and unit required)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response recordReading(SensorReading payload) {
        Sensor parentSensor = campusRegistry.getSensorById(ownerSensorId);

        if (parentSensor == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "No sensor was found with ID '" + ownerSensorId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        // Block recording when the device is unavailable
        String currentStatus = parentSensor.getStatus();
        if ("MAINTENANCE".equalsIgnoreCase(currentStatus) || "OFFLINE".equalsIgnoreCase(currentStatus)) {
            throw new SensorUnavailableException(
                    "Sensor '" + parentSensor.getName() + "' (ID: " + ownerSensorId +
                    ") is currently in '" + currentStatus + "' state and cannot accept new readings. " +
                    "Please wait until the device is back online before submitting data."
            );
        }

        // Persist the measurement with a server-generated UUID and epoch timestamp
        SensorReading capturedReading = new SensorReading(ownerSensorId, payload.getValue(), payload.getUnit());
        campusRegistry.addReading(ownerSensorId, capturedReading);

        // Side effect: synchronise the latest value on the parent sensor object
        parentSensor.setCurrentValue(payload.getValue());

        return Response.status(Response.Status.CREATED).entity(capturedReading).build();
    }
}
