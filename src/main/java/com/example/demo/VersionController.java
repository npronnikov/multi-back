package com.example.demo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final String version;

    public VersionController(@Value("${app.version:unknown}") String version) {
        this.version = version;
    }

    @GetMapping
    public Map<String, String> getVersion() {
        return Map.of("version", version);
    }
}
