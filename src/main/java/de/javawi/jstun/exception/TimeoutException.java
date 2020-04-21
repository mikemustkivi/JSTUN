package de.javawi.jstun.exception;

public class TimeoutException extends Exception {
    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }
}
