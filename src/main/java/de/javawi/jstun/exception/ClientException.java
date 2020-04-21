package de.javawi.jstun.exception;

public class ClientException extends Exception {
    public ClientException(String message) {
        super(message);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }
}
