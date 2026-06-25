package com.pactly.app.exception;

public class GmailAuthException extends RuntimeException {

    public GmailAuthException(String message) {
        super(message);
    }

    public GmailAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}