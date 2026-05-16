package com.monovai.domain.business.video.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.video.entity.VideoTemplate;

public interface VideoTemplateRepository extends JpaRepository<VideoTemplate, Long> {

	Optional<VideoTemplate> findByVideoSlug(String videoSlug);

	List<VideoTemplate> findAllByUser_IdAndSourceOrderByCreatedAtDesc(Long userId, String source);

	List<VideoTemplate> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
}
