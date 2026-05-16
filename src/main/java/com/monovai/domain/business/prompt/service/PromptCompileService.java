package com.monovai.domain.business.prompt.service;

import org.springframework.stereotype.Service;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
import com.monovai.domain.business.recommendation.entity.enums.Style;
import com.monovai.domain.business.recommendation.entity.value.GlobalLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptCompileService {

	private static final String RECOMMENDATION_SYSTEM_PROMPT_TEMPLATE = """
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

	public String compileRecommendationSystemPrompt(Style style) {
		return RECOMMENDATION_SYSTEM_PROMPT_TEMPLATE.formatted(
			style.getValue(), style.getLabel(), recommendationContextHint(style)
		);
	}

	public String compileRecommendationUserPrompt(
		String description, boolean hasProductImage, boolean hasReferenceImage
	) {
		StringBuilder sb = new StringBuilder();
		if (description != null && !description.isBlank()) {
			sb.append("[설명]\n").append(description).append("\n\n");
		}
		if (hasProductImage) {
			sb.append("(첨부 이미지: 사용자가 올린 제품 사진 — 1개 이상)\n");
		}
		if (hasReferenceImage) {
			sb.append("(첨부 이미지: 분위기 참고 이미지 — 1개 이상)\n");
		}
		if (sb.isEmpty()) {
			sb.append("자유롭게 추천해줘.");
		}
		return sb.toString();
	}

	public String compileImageGenerationPrompt(ImageJob job, ImageJobVariant variant) {
		GlobalLock lock = variant.getGlobalLock();

		StringBuilder sb = new StringBuilder();
		sb.append("Premium product photography.\n");
		sb.append("Concept: ").append(variant.getRecommendationTitle()).append(".\n");

		if (lock != null) {
			if (lock.background() != null) sb.append("Background: ").append(lock.background()).append(".\n");
			if (lock.surface() != null) sb.append("Surface: ").append(lock.surface()).append(".\n");
			if (lock.lighting() != null) sb.append("Lighting: ").append(lock.lighting()).append(".\n");
			if (lock.mood() != null) sb.append("Mood: ").append(lock.mood()).append(".\n");
		}

		if (job.getAngle() != null) {
			sb.append("Camera angle: ").append(job.getAngle().getValue()).append(".\n");
		}
		sb.append("Lighting style preference: ").append(job.getLighting().getValue()).append(".\n");
		sb.append("Aspect ratio: ").append(job.getRatio().getValue()).append(".\n");

		if (job.getDescription() != null && !job.getDescription().isBlank()) {
			sb.append("User description: ").append(job.getDescription()).append("\n");
		}
		sb.append("Constraints: no text overlay, no people, no logos.\n");
		return sb.toString();
	}

	/**
	 * Phase 3 빠른 수정 prompt 합성. v2.0:
	 * - TEXT_CREATE: base 없이 자유 생성
	 * - ANGLE_CHANGE: rotation/tilt 좌표 우선, fallback enum
	 * - INPAINT: 별도 워커에서 사용하지만 fallback 으로도 빌드 가능
	 */
	public String compileEditPrompt(ImageEdit edit) {
		EditParams p = edit.getParams() != null ? edit.getParams() : EditParams.empty();

		StringBuilder sb = new StringBuilder();

		boolean isTextCreate = edit.getMode() == com.monovai.domain.business.edit.entity.enums.EditMode.TEXT_CREATE;
		if (!isTextCreate) {
			sb.append("Image 1 is the source image. ");
			sb.append("CRITICAL pose lock — keep product identity, composition, and other locked attributes intact unless explicitly told otherwise:\n\n");
		}

		switch (edit.getMode()) {
			case BACKGROUND_CHANGE -> {
				sb.append("Edit type: BACKGROUND CHANGE.\n");
				sb.append("Change ONLY the background. Keep product, surface, lighting, and camera angle unchanged.\n");
				if (p.hasDescription()) sb.append("New background description: ").append(p.description()).append(".\n");
				if (p.hasReferenceImage()) sb.append("Use the reference image(s) as a style/mood reference for the new background.\n");
			}
			case LIGHTING_CHANGE -> {
				sb.append("Edit type: LIGHTING CHANGE.\n");
				sb.append("Change ONLY the lighting style. Keep product, background, surface, and angle unchanged.\n");
				if (p.lighting() != null) sb.append("New lighting style: ").append(p.lighting()).append(".\n");
			}
			case ANGLE_CHANGE -> {
				sb.append("Edit type: ANGLE CHANGE.\n");
				sb.append("Show the same product and scene from a different camera angle.\n");
				if (p.hasAngleCoordinates()) {
					double rot = p.rotation() != null ? p.rotation() : 0.0;
					double tilt = p.tilt() != null ? p.tilt() : 0.0;
					sb.append("Camera orbit (positive = right): ").append(rot).append(" degrees.\n");
					sb.append("Camera tilt (positive = look down): ").append(tilt).append(" degrees.\n");
				} else if (p.angle() != null) {
					sb.append("New camera angle: ").append(p.angle()).append(".\n");
				}
			}
			case RATIO_CHANGE -> {
				sb.append("Edit type: ASPECT RATIO CHANGE.\n");
				sb.append("Re-frame the same scene in a new aspect ratio. Extend background as needed.\n");
				if (p.ratio() != null) sb.append("New aspect ratio: ").append(p.ratio()).append(".\n");
			}
			case PRODUCT_REPLACE -> {
				int refCount = p.collectReferenceUrls().size();
				sb.append("Edit type: PRODUCT REPLACE.\n");
				if (refCount <= 1) {
					sb.append("Replace the product in Image 1 with the product shown in Image 2.\n");
				} else {
					sb.append("The ").append(refCount).append(" reference images each show a different product. ");
					sb.append("Compose them together naturally in the scene.\n");
				}
				sb.append("Keep the background, surface, lighting, and overall composition unchanged.\n");
			}
			case OBJECT_ADD -> {
				sb.append("Edit type: OBJECT ADD.\n");
				sb.append("Add an object/element to the scene without changing the existing product or environment.\n");
				if (p.hasDescription()) sb.append("Object to add: ").append(p.description()).append(".\n");
				if (p.hasReferenceImage()) sb.append("Use the reference image(s) for the object's appearance.\n");
			}
			case TEXT_CREATE -> {
				sb.append("Create a brand-new product photograph from the following description. ");
				sb.append("No base image — generate from scratch.\n");
				if (p.hasDescription()) sb.append("Description: ").append(p.description()).append("\n");
			}
			case INPAINT -> {
				sb.append("Edit type: INPAINT — only the masked area should change.\n");
				if (p.prompt() != null) sb.append("Edit intent: ").append(p.prompt()).append("\n");
			}
		}

		if (edit.getBaseGlobalLock() != null && !isTextCreate) {
			GlobalLock lock = edit.getBaseGlobalLock();
			sb.append("\n[Locked attributes — DO NOT change unless this edit explicitly targets them]\n");
			if (lock.background() != null) sb.append("- Background: ").append(lock.background()).append("\n");
			if (lock.surface() != null) sb.append("- Surface: ").append(lock.surface()).append("\n");
			if (lock.lighting() != null) sb.append("- Lighting: ").append(lock.lighting()).append("\n");
			if (lock.mood() != null) sb.append("- Mood: ").append(lock.mood()).append("\n");
		}

		sb.append("\nConstraints: no text overlay, no people, no logos.\n");
		return sb.toString();
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
