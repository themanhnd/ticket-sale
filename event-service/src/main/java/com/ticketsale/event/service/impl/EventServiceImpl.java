package com.ticketsale.event.service.impl;

import com.ticketsale.event.controller.dto.request.CreateEventRequest;
import com.ticketsale.event.controller.dto.response.EventResponse;
import com.ticketsale.event.repository.EventRepository;
import com.ticketsale.event.repository.entity.EventEntity;
import com.ticketsale.event.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        eventRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new IllegalArgumentException("Mã event đã tồn tại");
        });

        EventEntity saved = eventRepository.save(new EventEntity(request.code(), request.name(), request.location()));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(Long id) {
        EventEntity entity = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy event"));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getAll() {
        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EventResponse toResponse(EventEntity entity) {
        return new EventResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getLocation(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
