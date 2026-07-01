package com.ticketsale.event.service;

import com.ticketsale.event.controller.dto.request.CreateEventRequest;
import com.ticketsale.event.controller.dto.response.EventResponse;

import java.util.List;

public interface EventService {

    EventResponse create(CreateEventRequest request);

    EventResponse getById(Long id);

    List<EventResponse> getAll();
}