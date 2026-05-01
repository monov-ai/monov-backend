package com.monovai.domain.business.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.recommendation.dto.request.CreateRecommendationRequest;
import com.monovai.domain.business.recommendation.dto.response.RecommendationCreatedResponse;
import com.monovai.domain.business.recommendation.dto.response.RecommendationDetailResponse;
import com.monovai.domain.business.recommendation.repository.RecommendationRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationService {

	private final RecommendationRequestRepository requestRepository;

	@Transactional
	public RecommendationCreatedResponse create(Long userId, CreateRecommendationRequest request) {
		// TODO:
		// 1. style=STUDIO 면 productImageUrl 필수 검증 (400)
		// 2. requestSlug 생성 (bizrec_{timestamp}_{random6})
		// 3. User 조회 (NotFound 시 401)
		// 4. RecommendationRequest.create 저장 (PENDING)
		// 5. PromptCompileService 로 시스템 프롬프트 합성
		// 6. OpenAI Feign 호출
		// 7. 응답 → CorePoints + List<RecommendationItem> 으로 변환
		// 8. entity.completeWith(...) 호출 → JSON 컬럼에 자동 직렬화
		// 9. requestSlug 반환
		throw new UnsupportedOperationException("RecommendationService.create not implemented");
	}

	public RecommendationDetailResponse get(Long userId, String requestId) {
		// TODO:
		// 1. requestRepository.findByRequestSlug(requestId)
		// 2. 권한 검증 (entity.user.id == userId)
		// 3. RecommendationDetailResponse.of(entity) 반환
		throw new UnsupportedOperationException("RecommendationService.get not implemented");
	}
}