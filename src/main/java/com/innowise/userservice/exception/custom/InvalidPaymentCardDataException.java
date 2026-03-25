package com.innowise.userservice.exception.custom;

public class InvalidPaymentCardDataException extends RuntimeException {
    public InvalidPaymentCardDataException(String message) {
        super(message);
    }
}