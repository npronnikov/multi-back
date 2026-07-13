package com.example.demo.service;

import com.example.demo.dto.VersionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);
    private static final String VERSION_FILE = "src/main/resources/version.yml";

    @Value("${project.version}")
    private String version;

    public VersionResponse getVersion() {
        writeVersionToYaml();
        return new VersionResponse(version);
    }

    private void writeVersionToYaml() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, String> data = Map.of("version", version);
            mapper.writeValue(new File(VERSION_FILE), data);
        } catch (IOException e) {
            log.error("Failed to write version to YAML file", e);
        }
    }
}
