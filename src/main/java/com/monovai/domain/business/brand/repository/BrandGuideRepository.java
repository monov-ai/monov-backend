package com.monovai.domain.business.brand.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.brand.entity.BrandGuide;

public interface BrandGuideRepository extends JpaRepository<BrandGuide, Long> {

	Optional<BrandGuide> findByGuideSlug(String slug);

	List<BrandGuide> findAllByUser_IdOrderByIsDefaultDescCreatedAtDesc(Long userId);

	List<BrandGuide> findAllByUser_Id(Long userId);

	long countByUser_Id(Long userId);
}
