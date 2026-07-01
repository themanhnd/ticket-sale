package com.ticketsale.event.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.event.controller.dto.request.CreateEventRequest;
import com.ticketsale.event.controller.dto.response.EventResponse;
import com.ticketsale.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ApiResponse<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ApiResponse.ok(eventService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(eventService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<EventResponse>> getAll() {
        return ApiResponse.ok(eventService.getAll());
    }
}
