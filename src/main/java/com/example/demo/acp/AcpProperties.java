package com.example.demo.acp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acp")
public record AcpProperties(String command, List<String> arguments, String workingDirectory, boolean autoApprovePermissions) {

    public AcpProperties {
        if (command == null || command.isBlank()) {
            command = "qwen";
        }
        if (arguments == null || arguments.isEmpty()) {
            arguments = List.of("--acp");
        }
        if (workingDirectory == null || workingDirectory.isBlank()) {
            workingDirectory = System.getProperty("user.home");
        }
    }

    public List<String> processCommand() {
        List<String> full = new ArrayList<>();
        full.add(command);
        full.addAll(arguments);
        return full;
    }
}
