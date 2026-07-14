package com.example.demo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String version;

    @GetMapping("/api/version")
    public Map<String, String> getVersion() {
        return Map.of("version", version);
    }
}
