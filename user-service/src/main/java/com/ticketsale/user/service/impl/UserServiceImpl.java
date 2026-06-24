package com.ticketsale.user.service.impl;

import com.ticketsale.user.controller.dto.request.CreateUserRequest;
import com.ticketsale.user.controller.dto.response.UserResponse;
import com.ticketsale.user.repository.UserRepository;
import com.ticketsale.user.repository.entity.UserEntity;
import com.ticketsale.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Xử lý nghiệp vụ chính của user.
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("Email đã tồn tại");
        });

        UserEntity saved = userRepository.save(new UserEntity(request.email(), request.fullName()));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));
        return toResponse(entity);
    }

    // Chuyển Entity nội bộ thành DTO trả ra ngoài.
    private UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
