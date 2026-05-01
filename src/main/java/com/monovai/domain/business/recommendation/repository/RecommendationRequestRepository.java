package com.monovai.domain.business.recommendation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.recommendation.entity.RecommendationRequest;

public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long> {

	Optional<RecommendationRequest> findByRequestSlug(String requestSlug);
}