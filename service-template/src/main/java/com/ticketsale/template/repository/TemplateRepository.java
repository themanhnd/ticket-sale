package com.ticketsale.template.repository;

import com.ticketsale.template.repository.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Lớp truy cập bảng templates.
public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {

    Optional<TemplateEntity> findByCode(String code);
}