package com.example.demo.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.VersionDTO;

@RestController
public class VersionController {

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping("/api/version")
    public ResponseEntity<VersionDTO> getVersion() {
        String timestamp = Instant.now().toString();
        VersionDTO versionDTO = new VersionDTO(projectVersion, timestamp);
        return ResponseEntity.ok(versionDTO);
    }
}
