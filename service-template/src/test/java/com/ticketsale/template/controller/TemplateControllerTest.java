package com.ticketsale.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.template.controller.dto.response.TemplateResponse;
import com.ticketsale.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TemplateController.class)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemplateService templateService;

    @Test
    void createShouldReturnTemplate() throws Exception {
        Mockito.when(templateService.create(any()))
                .thenReturn(new TemplateResponse(
                        1L,
                        "DEMO",
                        "Demo template",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/templates")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TestRequest("DEMO", "Demo template"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private record TestRequest(String code, String name) {
    }
}