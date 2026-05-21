package com.monovai.domain.template.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.template.entity.TemplateFavorite;

public interface TemplateFavoriteRepository extends JpaRepository<TemplateFavorite, Long> {
	List<TemplateFavorite> findAllByUserId(Long userId);

	Optional<TemplateFavorite> findByUserIdAndTemplateId(Long userId, String templateId);
}
