package com.example.demo.acp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An ACP content block. Only the "text" variant is modeled since that is all this
 * client sends or renders; other variants (image, audio, resource) deserialize with
 * a null text and are skipped by callers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContentBlock(String type, String text) {

    public static ContentBlock text(String text) {
        return new ContentBlock("text", text);
    }
}
