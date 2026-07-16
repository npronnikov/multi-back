package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testVersionEndpointReturns200() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk());
    }

    @Test
    public void testVersionEndpointReturnsJson() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.backendVersion").exists())
                .andExpect(jsonPath("$.frontendVersion").exists());
    }

    @Test
    public void testVersionEndpointHasRequiredFields() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(jsonPath("$.backendVersion").isString())
                .andExpect(jsonPath("$.frontendVersion").isString());
    }

    @Test
    public void testVersionEndpointHasCacheHeaders() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(header().exists("Cache-Control"));
    }
}
