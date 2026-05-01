package com.monovai.domain.business.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.prompt.service.PromptCompileService;
import com.monovai.domain.business.recommendation.dto.request.CreateRecommendationRequest;
import com.monovai.domain.business.recommendation.dto.response.RecommendationCreatedResponse;
import com.monovai.domain.business.recommendation.dto.response.RecommendationDetailResponse;
import com.monovai.domain.business.recommendation.entity.RecommendationRequest;
import com.monovai.domain.business.recommendation.entity.enums.Style;
import com.monovai.domain.business.recommendation.repository.RecommendationRequestRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.external.openai.dto.GptRecommendationResponse;
import com.monovai.external.openai.service.OpenAiChatService;
import com.monovai.global.common.util.SlugGenerator;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.ForbiddenException;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationService {

	private static final String SLUG_PREFIX = "monov";
	private static final int EXPECTED_RECOMMENDATION_COUNT = 3;

	private final RecommendationRequestRepository requestRepository;
	private final UserRepository userRepository;
	private final OpenAiChatService openAiChatService;
	private final PromptCompileService promptCompileService;
	private final SlugGenerator slugGenerator;

	@Transactional
	public RecommendationCreatedResponse create(Long userId, CreateRecommendationRequest request) {
		// 1. style 변환 + 입력 검증
		Style style = Style.from(request.style());
		validateStudioRequiresProductImage(style, request.productImageUrl());

		// 2. User 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		// 3. RecommendationRequest 저장 (PENDING)
		RecommendationRequest entity = RecommendationRequest.create(
			slugGenerator.generate(SLUG_PREFIX),
			user,
			style,
			request.description(),
			request.productImageUrl(),
			request.productImagePath(),
			request.referenceImageUrl(),
			request.referenceImagePath()
		);
		entity = requestRepository.save(entity);

		// 4. GPT 호출
		String systemPrompt = promptCompileService.compileRecommendationSystemPrompt(style);
		String userPrompt = promptCompileService.compileRecommendationUserPrompt(
			request.description(), request.productImageUrl(), request.referenceImageUrl()
		);

		GptRecommendationResponse gpt;
		try {
			gpt = openAiChatService.generateRecommendations(systemPrompt, userPrompt);
		} catch (Exception e) {
			log.error("[Recommendation] GPT 호출 실패, requestSlug={}", entity.getRequestSlug(), e);
			entity.markFailed();
			throw new BadRequestException(ErrorCode.GPT_RESPONSE_INVALID);
		}

		// 5. 응답 검증 — 정확히 3건이어야 함
		if (gpt.recommendations() == null
			|| gpt.recommendations().size() != EXPECTED_RECOMMENDATION_COUNT) {
			log.warn("[Recommendation] GPT 응답 추천 수 불일치: expected={}, got={}",
				EXPECTED_RECOMMENDATION_COUNT,
				gpt.recommendations() == null ? 0 : gpt.recommendations().size());
			entity.markFailed();
			throw new BadRequestException(ErrorCode.GPT_RESPONSE_INVALID);
		}

		// 6. 본문을 entity 의 JSON 컬럼에 박제 + COMPLETED 로 전이
		entity.completeWith(gpt.headline(), gpt.summary(), gpt.corePoints(), gpt.recommendations());

		return RecommendationCreatedResponse.of(entity.getRequestSlug());
	}

	public RecommendationDetailResponse get(Long userId, String requestId) {
		RecommendationRequest entity = requestRepository.findByRequestSlug(requestId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.RECOMMENDATION_NOT_FOUND));

		if (!entity.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}

		return RecommendationDetailResponse.of(entity);
	}

	private void validateStudioRequiresProductImage(Style style, String productImageUrl) {
		if (style == Style.STUDIO && (productImageUrl == null || productImageUrl.isBlank())) {
			throw new BadRequestException(ErrorCode.STUDIO_PRODUCT_IMAGE_REQUIRED);
		}
	}
}
