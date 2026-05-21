package com.monovai.domain.brandkit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.brandkit.entity.BrandKit;

public interface BrandKitRepository extends JpaRepository<BrandKit, Long> {
	Optional<BrandKit> findByUser_Id(Long userId);
}
