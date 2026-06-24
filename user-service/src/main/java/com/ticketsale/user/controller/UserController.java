package com.ticketsale.user.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.user.controller.dto.request.CreateUserRequest;
import com.ticketsale.user.controller.dto.response.UserResponse;
import com.ticketsale.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

// API cho user-service.
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Tạo user mới.
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    // Lấy user theo id.
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }
}
