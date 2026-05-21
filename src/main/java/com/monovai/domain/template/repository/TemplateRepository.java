package com.monovai.domain.template.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.template.entity.Template;

public interface TemplateRepository extends JpaRepository<Template, String> {
	List<Template> findAllByOrderByCreatedAtDesc();
}
