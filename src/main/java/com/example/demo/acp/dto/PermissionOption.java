package com.example.demo.acp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** kind is one of: allow_once, allow_always, reject_once, reject_always. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionOption(String optionId, String name, String kind) {
}
