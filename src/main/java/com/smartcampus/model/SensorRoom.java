package com.smartcampus.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a physical campus room that hosts sensor devices.
 *
 * The sensorIds list acts as the "owned side" of the room-sensor relationship.
 * It is kept synchronised by the DataStore whenever a sensor is added or removed,
 * ensuring that querying either side of the relationship yields consistent results.
 */
public class SensorRoom {

    /** Server-generated UUID uniquely identifying this room. */
    private String id;

    /** Human-readable label displayed in dashboards (e.g. "West Wing Computing Lab"). */
    private String name;

    /** Physical location descriptor (e.g. "Block C, Level 1"). */
    private String location;

    /** Floor level within the building (0 = ground, negative values for basements). */
    private int floor;

    /** Maximum permitted occupancy for fire-safety compliance. */
    private int capacity;

    /** UUIDs of all sensor devices currently deployed in this room. */
    private List<String> sensorIds;

    /** ISO-8601 timestamp recorded at the moment the room was registered. */
    private String registeredAt;

    /** Default constructor required by Jackson for JSON deserialisation. */
    public SensorRoom() {
        this.sensorIds = new ArrayList<>();
    }

    /**
     * Full constructor used internally to create a server-managed room entity.
     *
     * @param name     display name of the room
     * @param location physical location string
     * @param floor    floor level within the building
     * @param capacity maximum permitted occupancy
     */
    public SensorRoom(String name, String location, int floor, int capacity) {
        this.id           = UUID.randomUUID().toString();
        this.name         = name;
        this.location     = location;
        this.floor        = floor;
        this.capacity     = capacity;
        this.sensorIds    = new ArrayList<>();
        this.registeredAt = LocalDateTime.now().toString();
    }

    // -------------------------------------------------------------------------
    // Convenience mutators for the sensor list
    // -------------------------------------------------------------------------

    /**
     * Appends a sensor UUID to this room's device list.
     *
     * @param sensorId UUID of the sensor to link
     */
    public void addSensorId(String sensorId) {
        if (this.sensorIds == null) {
            this.sensorIds = new ArrayList<>();
        }
        this.sensorIds.add(sensorId);
    }

    /**
     * Removes a sensor UUID from this room's device list.
     *
     * @param sensorId UUID of the sensor to unlink
     */
    public void removeSensorId(String sensorId) {
        if (this.sensorIds != null) {
            this.sensorIds.remove(sensorId);
        }
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getId()                        { return id; }
    public void   setId(String id)               { this.id = id; }

    public String getName()                      { return name; }
    public void   setName(String name)           { this.name = name; }

    public String getLocation()                  { return location; }
    public void   setLocation(String location)   { this.location = location; }

    public int    getFloor()                     { return floor; }
    public void   setFloor(int floor)            { this.floor = floor; }

    public int    getCapacity()                  { return capacity; }
    public void   setCapacity(int capacity)      { this.capacity = capacity; }

    public List<String> getSensorIds()           { return sensorIds; }
    public void setSensorIds(List<String> ids)   { this.sensorIds = ids; }

    public String getRegisteredAt()              { return registeredAt; }
    public void   setRegisteredAt(String ts)     { this.registeredAt = ts; }
}
