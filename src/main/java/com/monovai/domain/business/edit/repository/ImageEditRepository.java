package com.monovai.domain.business.edit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.edit.entity.ImageEdit;

public interface ImageEditRepository extends JpaRepository<ImageEdit, Long> {

	List<ImageEdit> findAllByRootJob_IdOrderByCreatedAtAsc(Long rootJobId);

	List<ImageEdit> findAllByRootJob_IdInOrderByCreatedAtAsc(List<Long> rootJobIds);

	Optional<ImageEdit> findByEditSlug(String editSlug);
}