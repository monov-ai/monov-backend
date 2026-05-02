package com.monovai.domain.business.imagejob.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.imagejob.entity.ImageJob;

public interface ImageJobRepository extends JpaRepository<ImageJob, Long> {

	Optional<ImageJob> findByJobSlug(String jobSlug);

	@EntityGraph(attributePaths = {"variants", "user", "request"})
	Optional<ImageJob> findWithVariantsByJobSlug(String jobSlug);
}