package com.smartcampus.exception;

import java.time.Instant;

/**
 * Standard error envelope returned by all exception mappers.
 *
 * Every error response shares this consistent JSON structure, making
 * it straightforward for clients to parse and handle API errors
 * programmatically regardless of the specific error type.
 *
 * Example payload:
 * {
 *   "httpStatus": 409,
 *   "errorCode":  "Conflict",
 *   "detail":     "Room 'West Wing Lab' still has 2 sensor(s) assigned.",
 *   "occurredAt": "2026-04-20T10:15:30.123Z"
 * }
 */
public class ErrorResponse {

    /** HTTP status code mirroring the response status line. */
    private int httpStatus;

    /** Short error category label (e.g. "Not Found", "Conflict"). */
    private String errorCode;

    /** Human-readable explanation of the specific problem. */
    private String detail;

    /** ISO-8601 UTC timestamp indicating when the error was generated. */
    private String occurredAt;

    /** Default constructor required by Jackson for JSON serialisation. */
    public ErrorResponse() {
    }

    /**
     * Convenience constructor that automatically records the current UTC time.
     *
     * @param httpStatus HTTP status code
     * @param errorCode  short error category label
     * @param detail     human-readable error explanation
     */
    public ErrorResponse(int httpStatus, String errorCode, String detail) {
        this.httpStatus  = httpStatus;
        this.errorCode   = errorCode;
        this.detail      = detail;
        this.occurredAt  = Instant.now().toString();
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public int    getHttpStatus()                    { return httpStatus; }
    public void   setHttpStatus(int httpStatus)      { this.httpStatus = httpStatus; }

    public String getErrorCode()                     { return errorCode; }
    public void   setErrorCode(String errorCode)     { this.errorCode = errorCode; }

    public String getDetail()                        { return detail; }
    public void   setDetail(String detail)           { this.detail = detail; }

    public String getOccurredAt()                    { return occurredAt; }
    public void   setOccurredAt(String occurredAt)   { this.occurredAt = occurredAt; }
}
