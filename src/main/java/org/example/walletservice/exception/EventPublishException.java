package org.example.walletservice.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // Marks the response with a 400 Bad Request status
public class EventPublishException extends RuntimeException {

    // Default constructor with a standard error message
    public EventPublishException() {
        super("Error Publishing Event");
    }

    // Constructor that allows a custom error message
    public EventPublishException(String message) {
        super(message);
    }

    // Constructor that allows a custom message and a cause (another throwable)
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}

