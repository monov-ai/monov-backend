package com.monovai.domain.brandkit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.brandkit.entity.BrandKitPlan;

public interface BrandKitPlanRepository extends JpaRepository<BrandKitPlan, Long> {
	Optional<BrandKitPlan> findByUserIdAndWeekId(Long userId, String weekId);
}
