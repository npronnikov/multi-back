package com.example.demo.acp.dto;

public record RequestPermissionResult(Outcome outcome) {

    public record Outcome(String outcome, String optionId) {
    }

    public static RequestPermissionResult selected(String optionId) {
        return new RequestPermissionResult(new Outcome("selected", optionId));
    }

    public static RequestPermissionResult cancelled() {
        return new RequestPermissionResult(new Outcome("cancelled", null));
    }
}
