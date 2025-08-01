package com.example.vivek.app.enums;

public enum TaskStatus {
    COMPLETED, // task completed
    IN_PROGRESS, // task in progress
    WAITING,// waiting for 3rd party to respond
    STOPPED, // stopped unexpectedly / failed
    CANCELLED // cancelled by user
}
