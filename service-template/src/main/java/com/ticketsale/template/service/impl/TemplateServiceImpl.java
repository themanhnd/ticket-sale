package com.ticketsale.template.service.impl;

import com.ticketsale.template.controller.dto.request.CreateTemplateRequest;
import com.ticketsale.template.controller.dto.response.TemplateResponse;
import com.ticketsale.template.repository.TemplateRepository;
import com.ticketsale.template.repository.entity.TemplateEntity;
import com.ticketsale.template.service.TemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Xử lý nghiệp vụ chính của template.
@Service
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateServiceImpl(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public TemplateResponse create(CreateTemplateRequest request) {
        templateRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new IllegalArgumentException("Mã đã tồn tại");
        });

        TemplateEntity saved = templateRepository.save(new TemplateEntity(request.code(), request.name()));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse getById(Long id) {
        TemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy template"));
        return toResponse(entity);
    }

    // Chuyển Entity nội bộ thành DTO trả ra ngoài.
    private TemplateResponse toResponse(TemplateEntity entity) {
        return new TemplateResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}