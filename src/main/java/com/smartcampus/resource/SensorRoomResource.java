package com.smartcampus.resource;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.RoomNotEmptyException;
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
 * JAX-RS resource class responsible for campus room management.
 *
 * Handles all CRUD operations under the /api/v1/rooms collection path.
 *
 * JAX-RS Lifecycle note: the runtime instantiates a brand-new instance of
 * this class for every incoming HTTP request (request-scoped). Shared state
 * is therefore maintained in the DataStore singleton rather than in instance
 * fields, preventing data loss between requests.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    /** Shared singleton data registry injected on each request. */
    private final DataStore campusRegistry = DataStore.getInstance();

    // -------------------------------------------------------------------------
    // GET /api/v1/rooms
    // -------------------------------------------------------------------------

    /**
     * Retrieves a complete list of all campus rooms.
     *
     * Returning full room objects rather than IDs alone allows clients to
     * display names, capacities, and sensor counts without issuing a separate
     * request per room — a conscious trade-off favouring reduced round-trips
     * over minimal payload size.
     */
    @GET
    public Response listAllRooms() {
        List<SensorRoom> campusRooms = campusRegistry.getAllRooms();
        return Response.ok(campusRooms).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/rooms/{roomId}
    // -------------------------------------------------------------------------

    /**
     * Retrieves detailed metadata for a single room identified by roomId.
     *
     * @param roomId path parameter containing the room UUID
     */
    @GET
    @Path("/{roomId}")
    public Response fetchRoomById(@PathParam("roomId") String roomId) {
        SensorRoom targetRoom = campusRegistry.getRoomById(roomId);

        if (targetRoom == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "No campus room was found with ID '" + roomId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        return Response.ok(targetRoom).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/rooms
    // -------------------------------------------------------------------------

    /**
     * Creates and registers a new campus room.
     *
     * Always generates a fresh UUID and creation timestamp — any ID supplied
     * in the request body is intentionally ignored to prevent client-imposed
     * ID clashes. Returns HTTP 201 Created with a Location header pointing to
     * the newly created resource.
     *
     * @param uriInfo injected context for building the Location URI
     * @param payload room properties supplied by the client
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(@Context UriInfo uriInfo, SensorRoom payload) {
        // Always generate a server-side ID — discard any client-provided value
        SensorRoom freshRoom = new SensorRoom(
                payload.getName(),
                payload.getLocation(),
                payload.getFloor(),
                payload.getCapacity()
        );
        campusRegistry.addRoom(freshRoom);

        URI locationUri = uriInfo.getAbsolutePathBuilder()
                .path(freshRoom.getId())
                .build();

        return Response.created(locationUri).entity(freshRoom).build();
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/rooms/{roomId}
    // -------------------------------------------------------------------------

    /**
     * Updates the mutable properties of an existing room.
     * The room ID, sensor list, and creation timestamp are preserved.
     *
     * @param roomId      path parameter containing the room UUID
     * @param updatedData replacement values supplied by the client
     */
    @PUT
    @Path("/{roomId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRoom(@PathParam("roomId") String roomId, SensorRoom updatedData) {
        SensorRoom existingRoom = campusRegistry.getRoomById(roomId);

        if (existingRoom == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "Cannot update: no campus room was found with ID '" + roomId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        // Selectively update only mutable fields
        if (updatedData.getName()     != null) existingRoom.setName(updatedData.getName());
        if (updatedData.getLocation() != null) existingRoom.setLocation(updatedData.getLocation());
        existingRoom.setFloor(updatedData.getFloor());
        existingRoom.setCapacity(updatedData.getCapacity());

        return Response.ok(existingRoom).build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId}
    // -------------------------------------------------------------------------

    /**
     * Decommissions a campus room from the registry.
     *
     * Business Rule: deletion is blocked when the room still has sensors
     * assigned to it, preventing orphaned sensor data. A RoomNotEmptyException
     * is raised in that case and mapped to HTTP 409 Conflict.
     *
     * Idempotency: the first successful DELETE removes the room and returns
     * HTTP 200. Subsequent calls for the same ID will find no room and return
     * HTTP 404. The net server state does not change after the initial removal,
     * satisfying the idempotency contract.
     *
     * @param roomId path parameter containing the room UUID
     */
    @DELETE
    @Path("/{roomId}")
    public Response decommissionRoom(@PathParam("roomId") String roomId) {
        SensorRoom targetRoom = campusRegistry.getRoomById(roomId);

        if (targetRoom == null) {
            ErrorResponse notFound = new ErrorResponse(
                    404, "Not Found",
                    "Cannot delete: no campus room was found with ID '" + roomId + "'."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
        }

        // Guard: refuse deletion when sensors are still linked to this room
        List<Sensor> linkedDevices = campusRegistry.getSensorsByRoomId(roomId);
        if (!linkedDevices.isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + targetRoom.getName() + "' (ID: " + roomId + ") cannot be deleted " +
                    "because " + linkedDevices.size() + " sensor(s) are still assigned to it. " +
                    "Please remove or reassign all sensors before decommissioning this room."
            );
        }

        campusRegistry.removeRoom(roomId);

        Map<String, Object> confirmation = new LinkedHashMap<>();
        confirmation.put("message", "Room successfully decommissioned.");
        confirmation.put("deletedRoomId", roomId);
        confirmation.put("deletedRoomName", targetRoom.getName());

        return Response.ok(confirmation).build();
    }
}
