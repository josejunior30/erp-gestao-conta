package com.juneba.erp.Exception;

public class UpstreamException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int httpStatus;
    private final String endpoint;
    private final String responseSnippet;

    public UpstreamException(int httpStatus, String endpoint, String responseSnippet) {
        super("Upstream HTTP " + httpStatus + " at " + endpoint);
        this.httpStatus = httpStatus;
        this.endpoint = endpoint;
        this.responseSnippet = responseSnippet;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getEndpoint() { return endpoint; }
    public String getResponseSnippet() { return responseSnippet; }
}