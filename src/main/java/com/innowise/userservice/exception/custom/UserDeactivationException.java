package com.innowise.userservice.exception.custom;

public class UserDeactivationException extends RuntimeException {
    public UserDeactivationException(String message) {
        super(message);
    }
}