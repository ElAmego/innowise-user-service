package com.innowise.userservice.exception.custom;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("User with email " + email + " already exists");
    }
}