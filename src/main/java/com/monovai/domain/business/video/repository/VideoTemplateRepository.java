package com.monovai.domain.business.video.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.video.entity.VideoTemplate;

public interface VideoTemplateRepository extends JpaRepository<VideoTemplate, Long> {

	Optional<VideoTemplate> findByVideoSlug(String videoSlug);

	List<VideoTemplate> findAllByUser_IdAndSourceOrderByCreatedAtDesc(Long userId, String source);

	List<VideoTemplate> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

	long countByUser_IdAndStatusIn(Long userId, java.util.Collection<com.monovai.domain.business.video.entity.enums.VideoStatus> statuses);

	java.util.List<VideoTemplate> findAllByUser_IdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(
		Long userId, java.sql.Timestamp since, org.springframework.data.domain.Pageable pageable);

	java.util.List<VideoTemplate> findAllByUser_IdOrderByUpdatedAtDesc(
		Long userId, org.springframework.data.domain.Pageable pageable);

	// §11: templateId 필터링
	java.util.List<VideoTemplate> findAllByUser_IdAndTemplateIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(
		Long userId, String templateId, java.sql.Timestamp since, org.springframework.data.domain.Pageable pageable);

	java.util.List<VideoTemplate> findAllByUser_IdAndTemplateIdOrderByUpdatedAtDesc(
		Long userId, String templateId, org.springframework.data.domain.Pageable pageable);
}
