package com.smartcampus.model;

import java.util.UUID;

/**
 * Represents a single measurement event captured by a sensor device.
 *
 * Reading objects are immutable in practice — once recorded they are never
 * modified. The timestamp is captured at construction time using the system
 * clock, providing a server-authoritative time rather than relying on
 * potentially inaccurate client clocks.
 */
public class SensorReading {

    /** Server-generated UUID uniquely identifying this measurement event. */
    private String id;

    /** UUID of the sensor that produced this reading. */
    private String sensorId;

    /** Numeric value of the measurement (e.g. 21.4 for temperature). */
    private double value;

    /** Engineering unit of the measurement (e.g. "degC", "ppm", "%RH"). */
    private String unit;

    /** Unix epoch milliseconds at which the reading was captured server-side. */
    private long capturedAt;

    /** Default constructor required by Jackson for JSON deserialisation. */
    public SensorReading() {
    }

    /**
     * Full constructor used internally to create a server-managed reading record.
     *
     * @param sensorId UUID of the parent sensor
     * @param value    measured numeric value
     * @param unit     engineering unit string
     */
    public SensorReading(String sensorId, double value, String unit) {
        this.id         = UUID.randomUUID().toString();
        this.sensorId   = sensorId;
        this.value      = value;
        this.unit       = unit;
        this.capturedAt = System.currentTimeMillis();
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getId()                        { return id; }
    public void   setId(String id)               { this.id = id; }

    public String getSensorId()                  { return sensorId; }
    public void   setSensorId(String sensorId)   { this.sensorId = sensorId; }

    public double getValue()                     { return value; }
    public void   setValue(double value)         { this.value = value; }

    public String getUnit()                      { return unit; }
    public void   setUnit(String unit)           { this.unit = unit; }

    public long   getCapturedAt()                { return capturedAt; }
    public void   setCapturedAt(long capturedAt) { this.capturedAt = capturedAt; }
}
