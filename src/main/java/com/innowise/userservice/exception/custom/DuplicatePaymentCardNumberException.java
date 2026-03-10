package com.innowise.userservice.exception.custom;

public class DuplicatePaymentCardNumberException extends RuntimeException {
    public DuplicatePaymentCardNumberException(String number) {
        super("Card with number " + number + " already exists");
    }
}