package com.sojka.pomeranian.chat.exception;

public class AstraConnectionException extends RuntimeException {
    public AstraConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
