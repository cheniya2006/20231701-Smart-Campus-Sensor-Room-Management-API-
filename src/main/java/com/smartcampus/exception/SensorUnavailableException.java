package com.smartcampus.exception;

/**
 * Thrown when a client attempts to post a measurement to a sensor that is
 * currently in MAINTENANCE or OFFLINE state.
 *
 * In these states, the physical hardware is disconnected from the network
 * and cannot produce valid readings. Accepting a fabricated value would
 * corrupt the sensor's historical data and mislead the currentValue field.
 *
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    /**
     * @param detail human-readable explanation including sensor name and current status
     */
    public SensorUnavailableException(String detail) {
        super(detail);
    }
}
