package com.example.demo.acp;

public class AcpException extends RuntimeException {

    public AcpException(String message) {
        super(message);
    }

    public AcpException(String message, Throwable cause) {
        super(message, cause);
    }
}
