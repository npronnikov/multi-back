package com.example.demo.service;

import com.example.demo.dto.VersionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VersionServiceTest {

    @Autowired
    private VersionService versionService;

    @Test
    void getVersion_shouldReturnVersionResponse() {
        VersionResponse response = versionService.getVersion();

        assertNotNull(response);
        assertNotNull(response.version());
        assertFalse(response.version().isEmpty());
    }
}
