package com.example.demo.controller;

import com.example.demo.dto.VersionResponse;
import com.example.demo.service.VersionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping("/version")
    public VersionResponse getVersion() {
        return versionService.getVersion();
    }
}
