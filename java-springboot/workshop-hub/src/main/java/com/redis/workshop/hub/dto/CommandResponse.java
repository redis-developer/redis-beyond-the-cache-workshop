package com.redis.workshop.hub.dto;

public class CommandResponse {
    private boolean success;
    private String message;
    private String output;

    public CommandResponse() {
    }

    public CommandResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public CommandResponse(boolean success, String message, String output) {
        this.success = success;
        this.message = message;
        this.output = output;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}

