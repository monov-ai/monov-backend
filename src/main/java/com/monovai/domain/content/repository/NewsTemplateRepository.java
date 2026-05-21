package com.monovai.domain.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.content.entity.NewsTemplate;

public interface NewsTemplateRepository extends JpaRepository<NewsTemplate, String> {
}
