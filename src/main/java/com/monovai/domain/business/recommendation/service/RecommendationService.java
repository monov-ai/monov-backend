package com.monovai.domain.business.recommendation.service;

import java.time.Duration;

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
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationService {

	private static final String SLUG_PREFIX = "monov";
	private static final int EXPECTED_RECOMMENDATION_COUNT = 3;
	private static final Duration GPT_IMAGE_TTL = Duration.ofMinutes(15);

	private final RecommendationRequestRepository requestRepository;
	private final UserRepository userRepository;
	private final OpenAiChatService openAiChatService;
	private final PromptCompileService promptCompileService;
	private final SlugGenerator slugGenerator;
	private final S3Service s3Service;

	@Transactional
	public RecommendationCreatedResponse create(Long userId, CreateRecommendationRequest request) {
		// 1. style 변환 + 입력 검증
		Style style = Style.from(request.style());
		validateStudioRequiresProductImage(style, request);

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

		// 4. GPT 호출 — multimodal 로 이미지 같이 전달
		String productImage = resolveImageUrlForGpt(request.productImageUrl(), request.productImagePath());
		String referenceImage = resolveImageUrlForGpt(request.referenceImageUrl(), request.referenceImagePath());

		String systemPrompt = promptCompileService.compileRecommendationSystemPrompt(style);
		String userPrompt = promptCompileService.compileRecommendationUserPrompt(
			request.description(), productImage != null, referenceImage != null
		);

		GptRecommendationResponse gpt;
		try {
			gpt = openAiChatService.generateRecommendations(
				systemPrompt, userPrompt, productImage, referenceImage
			);
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

	/**
	 * GPT 에 첨부할 이미지 URL 해석.
	 * - path(key) 가 있으면 우리 S3 의 짧은 TTL presigned URL 발급 (다른 사람 노출 안 됨)
	 * - 없고 url 만 있으면 외부 URL 그대로 사용 (예: unsplash)
	 * - 둘 다 없으면 null (이미지 없이 텍스트만)
	 */
	private String resolveImageUrlForGpt(String url, String key) {
		if (key != null && !key.isBlank()) {
			return s3Service.getPreSignedUrlForDownload(key, GPT_IMAGE_TTL);
		}
		if (url != null && !url.isBlank()) {
			return url;
		}
		return null;
	}

	private void validateStudioRequiresProductImage(Style style, CreateRecommendationRequest request) {
		if (style != Style.STUDIO) {
			return;
		}
		boolean hasUrl = request.productImageUrl() != null && !request.productImageUrl().isBlank();
		boolean hasPath = request.productImagePath() != null && !request.productImagePath().isBlank();
		if (!hasUrl && !hasPath) {
			throw new BadRequestException(ErrorCode.STUDIO_PRODUCT_IMAGE_REQUIRED);
		}
	}
}