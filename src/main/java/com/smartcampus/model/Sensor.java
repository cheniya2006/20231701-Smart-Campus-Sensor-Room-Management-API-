package com.smartcampus.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a sensor device deployed within a campus room.
 *
 * Each sensor belongs to exactly one room (via roomId) and records
 * measurements over time. The currentValue field is updated as a side
 * effect whenever a new reading is successfully posted, ensuring that
 * the most recent measurement is always available without querying
 * the full reading history.
 */
public class Sensor {

    /** Server-generated UUID uniquely identifying this sensor device. */
    private String id;

    /** UUID of the room where this sensor is physically installed. */
    private String roomId;

    /** Device category (e.g. "Temperature", "CO2", "Humidity", "Occupancy"). */
    private String type;

    /** Descriptive label used in dashboards (e.g. "Ambient Temp Monitor CL1-01"). */
    private String name;

    /**
     * Operational state of the device.
     * Allowed values: "ACTIVE", "MAINTENANCE", "OFFLINE".
     * Sensors in MAINTENANCE or OFFLINE state cannot accept new readings.
     */
    private String status;

    /** Most recent measurement captured by this device. Updated on each new reading. */
    private double currentValue;

    /** ISO-8601 timestamp recorded at the moment the sensor was registered. */
    private String registeredAt;

    /** Default constructor required by Jackson for JSON deserialisation. */
    public Sensor() {
    }

    /**
     * Full constructor used internally to create a server-managed sensor entity.
     *
     * @param roomId UUID of the room where the sensor is installed
     * @param type   device category string
     * @param name   descriptive display label
     */
    public Sensor(String roomId, String type, String name) {
        this.id           = UUID.randomUUID().toString();
        this.roomId       = roomId;
        this.type         = type;
        this.name         = name;
        this.status       = "ACTIVE";
        this.currentValue = 0.0;
        this.registeredAt = LocalDateTime.now().toString();
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getId()                           { return id; }
    public void   setId(String id)                  { this.id = id; }

    public String getRoomId()                       { return roomId; }
    public void   setRoomId(String roomId)          { this.roomId = roomId; }

    public String getType()                         { return type; }
    public void   setType(String type)              { this.type = type; }

    public String getName()                         { return name; }
    public void   setName(String name)              { this.name = name; }

    public String getStatus()                       { return status; }
    public void   setStatus(String status)          { this.status = status; }

    public double getCurrentValue()                 { return currentValue; }
    public void   setCurrentValue(double value)     { this.currentValue = value; }

    public String getRegisteredAt()                 { return registeredAt; }
    public void   setRegisteredAt(String ts)        { this.registeredAt = ts; }
}
