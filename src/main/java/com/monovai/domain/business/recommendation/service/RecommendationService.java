package com.monovai.domain.business.recommendation.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

	private static final String SLUG_PREFIX = "bizrec";
	private static final int EXPECTED_RECOMMENDATION_COUNT = 3;
	private static final int MAX_IMAGES_PER_SLOT = 4;
	private static final Duration GPT_IMAGE_TTL = Duration.ofMinutes(15);

	private final RecommendationRequestRepository requestRepository;
	private final UserRepository userRepository;
	private final OpenAiChatService openAiChatService;
	private final PromptCompileService promptCompileService;
	private final SlugGenerator slugGenerator;
	private final S3Service s3Service;

	@Transactional
	public RecommendationCreatedResponse create(Long userId, CreateRecommendationRequest request) {
		Style style = Style.from(request.style());

		List<String> productUrls = request.normalizedProductImageUrls();
		List<String> productPaths = request.normalizedProductImagePaths();
		List<String> referenceUrls = request.normalizedReferenceImageUrls();
		List<String> referencePaths = request.normalizedReferenceImagePaths();

		validateSlotCount(productUrls);
		validateSlotCount(productPaths);
		validateSlotCount(referenceUrls);
		validateSlotCount(referencePaths);
		validateStudioRequiresProductImage(style, productUrls, productPaths);

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		RecommendationRequest entity = RecommendationRequest.create(
			slugGenerator.generate(SLUG_PREFIX),
			user,
			style,
			request.description(),
			productUrls,
			productPaths,
			referenceUrls,
			referencePaths
		);
		entity = requestRepository.save(entity);

		List<String> productGptUrls = resolveImagesForGpt(productUrls, productPaths);
		List<String> referenceGptUrls = resolveImagesForGpt(referenceUrls, referencePaths);

		String systemPrompt = promptCompileService.compileRecommendationSystemPrompt(style);
		String userPrompt = promptCompileService.compileRecommendationUserPrompt(
			request.description(), !productGptUrls.isEmpty(), !referenceGptUrls.isEmpty()
		);

		GptRecommendationResponse gpt;
		try {
			gpt = openAiChatService.generateRecommendations(
				systemPrompt, userPrompt, productGptUrls, referenceGptUrls
			);
		} catch (Exception e) {
			log.error("[Recommendation] GPT 호출 실패, requestSlug={}", entity.getRequestSlug(), e);
			entity.markFailed();
			throw new BadRequestException(ErrorCode.GPT_RESPONSE_INVALID);
		}

		if (gpt.recommendations() == null
			|| gpt.recommendations().size() != EXPECTED_RECOMMENDATION_COUNT) {
			log.warn("[Recommendation] GPT 응답 추천 수 불일치: expected={}, got={}",
				EXPECTED_RECOMMENDATION_COUNT,
				gpt.recommendations() == null ? 0 : gpt.recommendations().size());
			entity.markFailed();
			throw new BadRequestException(ErrorCode.GPT_RESPONSE_INVALID);
		}

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

	private List<String> resolveImagesForGpt(List<String> urls, List<String> paths) {
		List<String> out = new ArrayList<>();
		if (paths != null) {
			for (String key : paths) {
				if (key != null && !key.isBlank()) {
					out.add(s3Service.getPreSignedUrlForDownload(key, GPT_IMAGE_TTL));
				}
			}
		}
		if (out.isEmpty() && urls != null) {
			for (String url : urls) {
				if (url != null && !url.isBlank()) out.add(url);
			}
		}
		return out;
	}

	private void validateSlotCount(List<String> list) {
		if (list != null && list.size() > MAX_IMAGES_PER_SLOT) {
			throw new BadRequestException(ErrorCode.TOO_MANY_IMAGES);
		}
	}

	private void validateStudioRequiresProductImage(Style style, List<String> productUrls, List<String> productPaths) {
		if (style != Style.STUDIO) return;
		boolean has = (productUrls != null && !productUrls.isEmpty())
			|| (productPaths != null && !productPaths.isEmpty());
		if (!has) {
			throw new BadRequestException(ErrorCode.STUDIO_PRODUCT_IMAGE_REQUIRED);
		}
	}
}
