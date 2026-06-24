package com.ticketsale.template.service;

import com.ticketsale.template.controller.dto.request.CreateTemplateRequest;
import com.ticketsale.template.controller.dto.response.TemplateResponse;

// Khai báo nghiệp vụ của template service.
public interface TemplateService {

    TemplateResponse create(CreateTemplateRequest request);

    TemplateResponse getById(Long id);
}