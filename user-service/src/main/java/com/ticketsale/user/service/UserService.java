package com.ticketsale.user.service;

import com.ticketsale.user.controller.dto.response.UserResponse;
import com.ticketsale.user.controller.dto.request.CreateUserRequest;

// Khai báo nghiệp vụ của template service.
public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse getById(Long id);
}