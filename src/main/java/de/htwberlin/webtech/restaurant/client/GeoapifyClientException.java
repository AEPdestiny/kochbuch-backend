package de.htwberlin.webtech.restaurant.client;

public class GeoapifyClientException extends RuntimeException {

    public GeoapifyClientException(String message) {
        super(message);
    }

    public GeoapifyClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
