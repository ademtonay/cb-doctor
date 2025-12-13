package com.cbdoctor.core.collector.rest;

public class MgmtRestException extends RuntimeException {
    public MgmtRestException(String message) {
        super(message);
    }

    public MgmtRestException(String message, Throwable cause) {
        super(message, cause);
    }
}
