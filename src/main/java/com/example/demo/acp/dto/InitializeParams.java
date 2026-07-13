package com.example.demo.acp.dto;

public record InitializeParams(int protocolVersion, ClientCapabilities clientCapabilities, ClientInfo clientInfo) {

    public record ClientInfo(String name, String version) {
    }

    public record ClientCapabilities(FsCapability fs, boolean terminal) {
    }

    public record FsCapability(boolean readTextFile, boolean writeTextFile) {
    }
}
