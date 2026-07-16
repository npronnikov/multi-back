package com.example.demo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VersionResponse {
    private String backendVersion;
    private String frontendVersion;
    private String gitCommitHash;
    private String buildTimestamp;
}
