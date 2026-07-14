package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.version=test-version")
class VersionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getVersion_returnsOkWithVersionJson() throws Exception {
		mockMvc.perform(get("/api/version"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value("test-version"));
	}

}
