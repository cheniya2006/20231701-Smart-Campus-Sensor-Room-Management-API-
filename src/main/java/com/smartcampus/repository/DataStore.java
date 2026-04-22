package com.smartcampus.repository;

import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.model.SensorRoom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe singleton registry acting as the central in-memory data store
 * for the Smart Campus API.
 *
 * Uses ConcurrentHashMap for all top-level collections to handle concurrent
 * access safely. Reading history lists are wrapped with
 * Collections.synchronizedList to prevent race conditions when multiple
 * requests append measurements simultaneously.
 *
 * Because JAX-RS creates a fresh resource-class instance per HTTP request
 * (request-scoped lifecycle), all resource classes obtain a reference to this
 * singleton so that every request operates on the same shared dataset.
 */
public class DataStore {

    /** The single shared instance of this registry. */
    private static DataStore instance;

    /** Primary registry of campus rooms, keyed by room UUID. */
    private final Map<String, SensorRoom> roomRegistry = new ConcurrentHashMap<>();

    /** Primary registry of sensor devices, keyed by sensor UUID. */
    private final Map<String, Sensor> sensorRegistry = new ConcurrentHashMap<>();

    /**
     * Historical measurement logs per sensor.
     * Keyed by sensor UUID; each value is a thread-safe list of readings.
     */
    private final Map<String, List<SensorReading>> measurementLog = new ConcurrentHashMap<>();

    /** Prevent external instantiation — use getInstance() instead. */
    private DataStore() {
        seedInitialData();
    }

    /**
     * Returns the singleton DataStore instance, initialising it on first call.
     * Synchronised to guarantee safe publication across threads.
     */
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // =========================================================================
    // Initial Data Seed
    // =========================================================================

    /**
     * Populates the registry with realistic demonstration data so that
     * the API returns meaningful responses immediately after start-up.
     */
    private void seedInitialData() {

        // --- Rooms -----------------------------------------------------------
        SensorRoom labWest    = new SensorRoom("West Wing Computing Lab",   "Block C, Level 1",  1, 40);
        SensorRoom auditorium = new SensorRoom("Main Auditorium",           "Block A, Ground",   0, 350);
        SensorRoom dataCenter = new SensorRoom("Network Operations Centre", "Block B, Basement", 0, 8);

        roomRegistry.put(labWest.getId(),    labWest);
        roomRegistry.put(auditorium.getId(), auditorium);
        roomRegistry.put(dataCenter.getId(), dataCenter);

        // --- Sensors ---------------------------------------------------------
        Sensor tempLabWest   = new Sensor(labWest.getId(),    "Temperature", "Ambient Temp Monitor CL1-01");
        Sensor co2LabWest    = new Sensor(labWest.getId(),    "CO2",         "Air Quality Probe CL1-02");
        Sensor occAuditorium = new Sensor(auditorium.getId(), "Occupancy",   "Crowd Density Tracker A-01");
        Sensor tempDC        = new Sensor(dataCenter.getId(), "Temperature", "Server Hall Thermal Guard B-01");
        tempDC.setStatus("MAINTENANCE"); // Offline for scheduled servicing — useful for 403 demo

        sensorRegistry.put(tempLabWest.getId(),   tempLabWest);
        sensorRegistry.put(co2LabWest.getId(),    co2LabWest);
        sensorRegistry.put(occAuditorium.getId(), occAuditorium);
        sensorRegistry.put(tempDC.getId(),        tempDC);

        // Keep parent rooms' sensorIds lists consistent
        labWest.addSensorId(tempLabWest.getId());
        labWest.addSensorId(co2LabWest.getId());
        auditorium.addSensorId(occAuditorium.getId());
        dataCenter.addSensorId(tempDC.getId());

        // --- Initialise thread-safe measurement log for every sensor --------
        measurementLog.put(tempLabWest.getId(),   Collections.synchronizedList(new ArrayList<>()));
        measurementLog.put(co2LabWest.getId(),    Collections.synchronizedList(new ArrayList<>()));
        measurementLog.put(occAuditorium.getId(), Collections.synchronizedList(new ArrayList<>()));
        measurementLog.put(tempDC.getId(),        Collections.synchronizedList(new ArrayList<>()));

        // Seed historical readings so the history endpoint is not empty
        SensorReading warmReading = new SensorReading(tempLabWest.getId(), 21.4, "degC");
        SensorReading co2Reading  = new SensorReading(co2LabWest.getId(),  412.0, "ppm");
        measurementLog.get(tempLabWest.getId()).add(warmReading);
        measurementLog.get(co2LabWest.getId()).add(co2Reading);

        // Reflect the latest values on the parent sensor objects
        tempLabWest.setCurrentValue(21.4);
        co2LabWest.setCurrentValue(412.0);
    }

    // =========================================================================
    // Room Operations
    // =========================================================================

    /** Returns a snapshot list of all rooms currently in the registry. */
    public List<SensorRoom> getAllRooms() {
        return new ArrayList<>(roomRegistry.values());
    }

    /**
     * Looks up a room by its unique identifier.
     *
     * @param roomId UUID of the target room
     * @return the matching SensorRoom, or null if not found
     */
    public SensorRoom getRoomById(String roomId) {
        return roomRegistry.get(roomId);
    }

    /**
     * Checks whether a room with the given ID is registered.
     *
     * @param roomId UUID to verify
     * @return true if the room exists, false otherwise
     */
    public boolean roomExists(String roomId) {
        return roomRegistry.containsKey(roomId);
    }

    /**
     * Registers a new room in the campus registry.
     *
     * @param room the fully-constructed SensorRoom to store
     */
    public void addRoom(SensorRoom room) {
        roomRegistry.put(room.getId(), room);
    }

    /**
     * Removes the room with the given ID from the registry.
     *
     * @param roomId UUID of the room to remove
     * @return the removed SensorRoom, or null if it did not exist
     */
    public SensorRoom removeRoom(String roomId) {
        return roomRegistry.remove(roomId);
    }

    // =========================================================================
    // Sensor Operations
    // =========================================================================

    /** Returns a snapshot list of all sensor devices in the registry. */
    public List<Sensor> getAllSensors() {
        return new ArrayList<>(sensorRegistry.values());
    }

    /**
     * Looks up a sensor device by its unique identifier.
     *
     * @param sensorId UUID of the target sensor
     * @return the matching Sensor, or null if not found
     */
    public Sensor getSensorById(String sensorId) {
        return sensorRegistry.get(sensorId);
    }

    /**
     * Registers a new sensor and initialises its measurement log.
     * Also updates the parent room's sensorIds list to maintain
     * referential consistency across the in-memory data model.
     *
     * @param device the fully-constructed Sensor to store
     */
    public void addSensor(Sensor device) {
        sensorRegistry.put(device.getId(), device);
        // Initialise an empty (thread-safe) reading list for the new device
        measurementLog.putIfAbsent(device.getId(), Collections.synchronizedList(new ArrayList<>()));
        // Register the sensor ID on the parent room
        SensorRoom parentRoom = roomRegistry.get(device.getRoomId());
        if (parentRoom != null) {
            parentRoom.addSensorId(device.getId());
        }
    }

    /**
     * Removes a sensor, clears its measurement log, and updates the parent
     * room's sensorIds list to prevent stale references.
     *
     * @param sensorId UUID of the sensor to remove
     * @return the removed Sensor, or null if it did not exist
     */
    public Sensor removeSensor(String sensorId) {
        Sensor target = sensorRegistry.get(sensorId);
        if (target != null) {
            SensorRoom parentRoom = roomRegistry.get(target.getRoomId());
            if (parentRoom != null) {
                parentRoom.removeSensorId(sensorId);
            }
        }
        measurementLog.remove(sensorId);
        return sensorRegistry.remove(sensorId);
    }

    /**
     * Returns all sensors currently assigned to the specified room.
     *
     * @param roomId UUID of the parent room
     * @return list of Sensor objects belonging to that room
     */
    public List<Sensor> getSensorsByRoomId(String roomId) {
        return sensorRegistry.values().stream()
                .filter(s -> roomId.equals(s.getRoomId()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all sensors whose type field matches the given value
     * using a case-insensitive comparison.
     *
     * @param sensorType the device category to filter by (e.g. "CO2")
     * @return filtered list of matching Sensor objects
     */
    public List<Sensor> getSensorsByType(String sensorType) {
        return sensorRegistry.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(sensorType))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Reading Operations
    // =========================================================================

    /**
     * Retrieves a snapshot of the complete measurement history for a sensor.
     *
     * @param sensorId UUID of the target sensor
     * @return list of SensorReading objects (may be empty, never null)
     */
    public List<SensorReading> getReadingsBySensorId(String sensorId) {
        List<SensorReading> history = measurementLog.get(sensorId);
        if (history == null) {
            return new ArrayList<>();
        }
        // Return a defensive snapshot to avoid ConcurrentModificationException
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /**
     * Appends a new measurement to the sensor's reading history.
     *
     * @param sensorId UUID of the sensor that produced the reading
     * @param reading  the SensorReading to record
     */
    public void addReading(String sensorId, SensorReading reading) {
        List<SensorReading> history = measurementLog
                .computeIfAbsent(sensorId, k -> Collections.synchronizedList(new ArrayList<>()));
        history.add(reading);
    }
}
