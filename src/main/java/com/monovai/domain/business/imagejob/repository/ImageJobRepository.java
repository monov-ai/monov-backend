package com.monovai.domain.business.imagejob.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.imagejob.entity.ImageJob;

public interface ImageJobRepository extends JpaRepository<ImageJob, Long> {

	Optional<ImageJob> findByJobSlug(String jobSlug);

	@EntityGraph(attributePaths = {"variants", "user", "request"})
	Optional<ImageJob> findWithVariantsByJobSlug(String jobSlug);

	@EntityGraph(attributePaths = {"variants", "request"})
	List<ImageJob> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	long countByUser_IdAndStatusIn(Long userId, java.util.Collection<com.monovai.domain.business.imagejob.entity.enums.JobStatus> statuses);
}
