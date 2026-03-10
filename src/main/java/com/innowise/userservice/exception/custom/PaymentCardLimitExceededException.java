package com.innowise.userservice.exception.custom;

public class PaymentCardLimitExceededException extends RuntimeException {
    public PaymentCardLimitExceededException(Long userId, int currentCount, int maxCount) {
        super(String.format("User %d already has %d cards (maximum %d)", userId, currentCount, maxCount));
    }
}