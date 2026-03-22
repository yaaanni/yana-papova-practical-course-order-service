package com.example.OrderService.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {

        super(message);
    }
}