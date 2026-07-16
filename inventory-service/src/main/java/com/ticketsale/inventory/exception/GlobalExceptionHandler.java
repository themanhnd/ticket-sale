package com.ticketsale.inventory.exception;

import com.ticketsale.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

// Chuẩn hóa lỗi trả ra API.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Trả lỗi khi request DTO vi phạm validation.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");

        return ApiResponse.fail(message);
    }

    // Trả lỗi nghiệp vụ đơn giản.
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }
}