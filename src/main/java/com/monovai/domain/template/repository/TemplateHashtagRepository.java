package com.monovai.domain.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.template.entity.TemplateHashtag;

public interface TemplateHashtagRepository extends JpaRepository<TemplateHashtag, Long> {
}
