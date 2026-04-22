package com.smartcampus.exception;

/**
 * Thrown when a client attempts to decommission a campus room that still has
 * sensor devices assigned to it.
 *
 * Deleting a room with active sensors would create orphaned sensor records
 * with a dangling roomId reference. This exception enforces the business rule
 * that all sensors must be removed or reassigned before the room can be deleted.
 *
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    /**
     * @param detail human-readable explanation including room name and sensor count
     */
    public RoomNotEmptyException(String detail) {
        super(detail);
    }
}
