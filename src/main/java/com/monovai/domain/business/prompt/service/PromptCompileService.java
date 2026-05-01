package com.monovai.domain.business.prompt.service;

import org.springframework.stereotype.Service;

import com.monovai.domain.business.recommendation.entity.enums.Style;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 1·2·3 모두 사용하는 프롬프트 합성 책임.
 * v1.1: banner-event, source-image 시스템 프롬프트 강화.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptCompileService {

	private static final String SYSTEM_PROMPT_TEMPLATE = """
		너는 제품 사진 컨셉을 추천하는 AI 디렉터다.
		사용자가 입력한 정보를 바탕으로 정확히 3개의 컨셉 추천을 만든다.

		[현재 스타일]
		%s — %s
		%s

		[작성 규칙]
		- title, description, tags 는 한국어로 작성
		- id 는 영문 snake_case 로 짧고 의미있게 (예: warm_wood_studio, stone_luxury_studio)
		- globalLock 의 4개 필드(background, surface, lighting, mood) 는 영문으로 (이미지 생성 워커가 그대로 prompt 에 사용)
		- recommended 는 셋 중 가장 추천하는 1건만 true, 나머지는 false
		- 3개의 추천이 서로 다른 분위기/방향을 가지도록 다양성 확보
		- corePoints.summary 와 corePoints.keywords 는 사용자가 입력한 핵심 키워드를 정리

		응답 형식은 시스템이 자동으로 안내한다 (record 매핑).
		""";

	/**
	 * Phase 1 시스템 프롬프트.
	 */
	public String compileRecommendationSystemPrompt(Style style) {
		String label = style.getLabel();
		String contextHint = recommendationContextHint(style);
		return SYSTEM_PROMPT_TEMPLATE.formatted(style.getValue(), label, contextHint);
	}

	/**
	 * Phase 1 사용자 프롬프트.
	 * 다중 이미지 (제품/참고) URL 은 텍스트로 포함. 추후 multimodal (image input) 로 업그레이드 가능.
	 */
	public String compileRecommendationUserPrompt(
		String description, String productImageUrl, String referenceImageUrl
	) {
		StringBuilder sb = new StringBuilder();
		if (description != null && !description.isBlank()) {
			sb.append("[설명]\n").append(description).append("\n\n");
		}
		if (productImageUrl != null && !productImageUrl.isBlank()) {
			sb.append("[제품 이미지 URL]\n").append(productImageUrl).append("\n\n");
		}
		if (referenceImageUrl != null && !referenceImageUrl.isBlank()) {
			sb.append("[참고 이미지 URL]\n").append(referenceImageUrl).append("\n\n");
		}
		if (sb.isEmpty()) {
			sb.append("자유롭게 추천해줘.");
		}
		return sb.toString();
	}

	/**
	 * Phase 2: 선택된 추천 + 옵션으로 Nanobanana prompt 합성. (TODO)
	 */
	public String compileImageGenerationPrompt(String compiledRecommendationPrompt, String optionsJson) {
		throw new UnsupportedOperationException("compileImageGenerationPrompt not implemented");
	}

	/**
	 * Phase 3: 빠른 수정 prompt 합성. (TODO)
	 */
	public String compileEditPrompt(String baseImageUrl, String editType, String userPrompt) {
		throw new UnsupportedOperationException("compileEditPrompt not implemented");
	}

	private String recommendationContextHint(Style style) {
		return switch (style) {
			case STUDIO -> """
				- 깔끔한 스튜디오 환경에서 제품을 단독 부각
				- 배경/표면/조명을 정교하게 통제
				- 광고 / 상세 페이지에 적합한 정적 컷
				""";
			case BANNER_EVENT -> """
				- 캠페인/배너용 풍부한 디렉션
				- 분위기/시즈널 컨텍스트 (예: 봄 신상, 블랙프라이데이) 반영
				- 텍스트 / 카피 / 인물 묘사 금지 (워커 lock)
				""";
			case SOURCE_IMAGE -> """
				- 사용자가 올린 원본 이미지를 변형해 재사용 가능한 asset 생성
				- 구도 / 핵심 오브젝트 일관성 유지
				""";
			case FREEFORM -> """
				- 자유로운 컨셉 시도
				- 사용자 설명을 최대한 반영하면서 다양한 방향 제시
				""";
		};
	}
}