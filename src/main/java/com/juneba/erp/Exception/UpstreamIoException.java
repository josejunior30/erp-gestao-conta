package com.juneba.erp.Exception;

public class UpstreamIoException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String endpoint;

    public UpstreamIoException(String endpoint, Throwable cause) {
        super("Upstream I/O at " + endpoint + ": " + (cause != null ? cause.getMessage() : "unknown"), cause);
        this.endpoint = endpoint;
    }

    public String getEndpoint() { return endpoint; }
}