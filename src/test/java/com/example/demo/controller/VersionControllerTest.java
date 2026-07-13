package com.example.demo.controller;

import com.example.demo.dto.VersionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VersionControllerTest {

    @Autowired
    private VersionController versionController;

    @Test
    void getVersion_shouldReturnVersionResponse() {
        VersionResponse response = versionController.getVersion();

        assertNotNull(response);
        assertNotNull(response.version());
        assertFalse(response.version().isEmpty());
    }
}
