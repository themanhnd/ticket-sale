package com.ticketsale.template.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Dữ liệu frontend gửi lên khi tạo template.
public record CreateTemplateRequest(
        @NotBlank(message = "Mã không được để trống")
        @Size(max = 100, message = "Mã tối đa 100 ký tự")
        String code,

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 255, message = "Tên tối đa 255 ký tự")
        String name
) {
}