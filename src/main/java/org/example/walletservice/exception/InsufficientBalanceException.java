package org.example.walletservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.BAD_REQUEST) // Marks the response with a 400 Bad Request status
public class InsufficientBalanceException extends RuntimeException {

    private final String detailMessage;

    // Default constructor with a standard error message
    public InsufficientBalanceException() {
        super("Insufficient Balance Exception");
        this.detailMessage = "Insufficient Balance Exception";
    }

    // Constructor that allows a custom error message
    public InsufficientBalanceException(String detailMessage) {
        super(detailMessage);
        this.detailMessage = detailMessage;
    }

    // Constructor that allows a custom message and a cause (another throwable)
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.detailMessage = message;
    }
}
