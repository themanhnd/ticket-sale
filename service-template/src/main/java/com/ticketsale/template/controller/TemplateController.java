package com.ticketsale.template.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.template.controller.dto.request.CreateTemplateRequest;
import com.ticketsale.template.controller.dto.response.TemplateResponse;
import com.ticketsale.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

// API mẫu cho service clone từ service-template.
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    // Tạo template mới.
    @PostMapping
    public ApiResponse<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.ok(templateService.create(request));
    }

    // Lấy template theo id.
    @GetMapping("/{id}")
    public ApiResponse<TemplateResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(templateService.getById(id));
    }
}