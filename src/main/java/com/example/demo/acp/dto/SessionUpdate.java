package com.example.demo.acp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.JsonNode;

/**
 * One entry of the "update" payload carried by a "session/update" notification.
 * The wire discriminator is the "sessionUpdate" property. Variants this client does not
 * act on (e.g. available_commands_update, current_mode_update) fall back to {@link Unknown}
 * so unrecognized future update kinds do not break deserialization.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "sessionUpdate", defaultImpl = SessionUpdate.Unknown.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SessionUpdate.AgentMessageChunk.class, name = "agent_message_chunk"),
        @JsonSubTypes.Type(value = SessionUpdate.AgentThoughtChunk.class, name = "agent_thought_chunk"),
        @JsonSubTypes.Type(value = SessionUpdate.UserMessageChunk.class, name = "user_message_chunk"),
        @JsonSubTypes.Type(value = SessionUpdate.ToolCall.class, name = "tool_call"),
        @JsonSubTypes.Type(value = SessionUpdate.ToolCallUpdate.class, name = "tool_call_update"),
        @JsonSubTypes.Type(value = SessionUpdate.Plan.class, name = "plan"),
})
public sealed interface SessionUpdate {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AgentMessageChunk(ContentBlock content) implements SessionUpdate {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AgentThoughtChunk(ContentBlock content) implements SessionUpdate {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserMessageChunk(ContentBlock content) implements SessionUpdate {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ToolCall(String toolCallId, String title, String kind, String status) implements SessionUpdate {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ToolCallUpdate(String toolCallId, String status) implements SessionUpdate {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Plan(List<JsonNode> entries) implements SessionUpdate {
    }

    /** Fallback for update kinds this client does not need to react to. */
    record Unknown() implements SessionUpdate {
    }
}
