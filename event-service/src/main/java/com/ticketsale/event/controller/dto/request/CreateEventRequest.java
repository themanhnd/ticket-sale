package com.ticketsale.event.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
        @NotBlank(message = "Mã event không được để trống")
        @Size(max = 100, message = "Mã event tối đa 100 ký tự")
        String code,

        @NotBlank(message = "Tên event không được để trống")
        @Size(max = 255, message = "Tên event tối đa 255 ký tự")
        String name,

        @NotBlank(message = "Địa điểm không được để trống")
        @Size(max = 255, message = "Địa điểm tối đa 255 ký tự")
        String location
) {
}
