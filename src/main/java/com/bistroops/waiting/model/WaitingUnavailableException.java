package com.bistroops.waiting.model;

public class WaitingUnavailableException extends RuntimeException {

    public WaitingUnavailableException(
            String message, Throwable cause) {

        super(message, cause);
    }
}