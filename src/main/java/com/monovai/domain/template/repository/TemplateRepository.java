package com.monovai.domain.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.template.entity.Template;

public interface TemplateRepository extends JpaRepository<Template, Long> {
}
