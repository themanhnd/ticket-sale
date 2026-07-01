package com.ticketsale.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.event.controller.dto.response.EventResponse;
import com.ticketsale.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void createShouldReturnEvent() throws Exception {
        Mockito.when(eventService.create(any()))
                .thenReturn(new EventResponse(
                        1L,
                        "MUSIC_NIGHT_01",
                        "Music Night 2026",
                        "Ho Chi Minh City",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TestRequest(
                                "MUSIC_NIGHT_01",
                                "Music Night 2026",
                                "Ho Chi Minh City"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("MUSIC_NIGHT_01"));
    }

    @Test
    void getAllShouldReturnEvents() throws Exception {
        Mockito.when(eventService.getAll())
                .thenReturn(List.of(new EventResponse(
                        1L,
                        "MUSIC_NIGHT_01",
                        "Music Night 2026",
                        "Ho Chi Minh City",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("MUSIC_NIGHT_01"));
    }

    private record TestRequest(String code, String name, String location) {
    }
}
