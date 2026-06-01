package com.monovai.domain.business.prompt.service;

import org.springframework.stereotype.Service;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
import com.monovai.domain.business.recommendation.entity.enums.Style;
import com.monovai.domain.business.recommendation.entity.value.GlobalLock;
import com.monovai.external.openai.service.OpenAiChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptCompileService {

	private final OpenAiChatService openAiChatService;

	private static final String PHASE2_SYSTEM_PROMPT = """
		You are a senior prompt engineer specialized in product photography prompts for Google Nanobanana
		(image generation). Take the user's structured concept brief and produce a single English
		generation prompt rich in visual vocabulary (lens, lighting, composition, surface, material,
		mood) so the image model has enough context to render a premium result.

		Constraints:
		- Single paragraph, English only, 60–140 words.
		- No bullet lists. No JSON. No markdown.
		- Do NOT add people, text overlays, logos, watermarks, or brand names.
		- Honor the aspect ratio, lighting style, and locked attributes (background/surface/mood) exactly.
		- Lead with the subject and concept; end with constraint clause "no text, no people, no logos".
		""";

	private static final String RECOMMENDATION_SYSTEM_PROMPT_TEMPLATE = """
		너는 제품 사진 컨셉을 추천하는 AI 디렉터다.
		사용자가 입력한 정보를 바탕으로 정확히 3개의 컨셉 추천을 만든다.

		[현재 스타일]
		%s — %s
		%s

		[TYPE LOCK — 스타일별 절대 강제]
		%s

		[작성 규칙]
		- title, description, tags 는 한국어로 작성
		- id 는 영문 snake_case 로 짧고 의미있게 (예: warm_wood_studio, stone_luxury_studio)
		- globalLock 의 4개 필드(background, surface, lighting, mood) 는 영문으로 (이미지 생성 워커가 그대로 prompt 에 사용)
		- recommended 는 셋 중 가장 추천하는 1건만 true, 나머지는 false
		- corePoints.summary 와 corePoints.keywords 는 사용자가 입력한 핵심 키워드를 정리

		[CUT RULE — 추천 3개의 차별화]
		- 3개 추천은 background / surface / lighting / mood 중 최소 2개 축에서 서로 달라야 한다
		- 동일한 단어 (예: wood, marble) 를 두 추천에 동시에 쓰지 말 것
		- mood 톤이 모두 같은 방향(전부 warm, 전부 cool) 이면 안 됨 — 최소 1개는 대비되는 방향

		[GLOBAL HARD RULES — 안전 가드]
		- 사람, 인물, 얼굴, 손, 신체 부위 묘사 금지
		- 텍스트/카피/로고/워터마크/브랜드명 묘사 금지
		- 폭력, 성적, 정치적, 의학적 컨텍스트 금지

		응답 형식은 시스템이 자동으로 안내한다 (record 매핑).
		""";

	public String compileRecommendationSystemPrompt(Style style) {
		return RECOMMENDATION_SYSTEM_PROMPT_TEMPLATE.formatted(
			style.getValue(), style.getLabel(), recommendationContextHint(style), typeLock(style)
		);
	}

	private String typeLock(Style style) {
		return switch (style) {
			case STUDIO -> """
				- 카테고리 LOCK: studio product shot (광고/상세페이지 단독 컷)
				- 카메라: 제품 단독 부각, 정면/반측면 위주
				- 배경: 단색 또는 텍스처 한 가지로 통일 (clutter 금지)
				- 추가 오브젝트는 1~2개로 절제, 절대 주인공 흐리지 않게
				""";
			case BANNER_EVENT -> """
				- 카테고리 LOCK: campaign banner / event key visual
				- 시즈널 모티브 (계절, 행사, 프로모션) 반드시 1개 포함
				- 구도는 가로형/세로형 배너 친화 (여백, breathing room 확보)
				- 배경은 풍부한 디렉션 가능하나 인물/텍스트는 금지 (워커 lock)
				""";
			case SOURCE_IMAGE -> """
				- 카테고리 LOCK: source-image faithful transform
				- 사용자가 올린 원본 이미지의 핵심 오브젝트 / 구도 / 비례는 반드시 유지
				- 배경, 조명, 분위기만 변형 — 메인 제품은 그대로
				- 새로운 오브젝트를 추가하지 말 것 (원본 충실도 최우선)
				""";
			case FREEFORM -> """
				- 카테고리 LOCK: free creative direction
				- 사용자 설명을 최대한 반영하면서 3개를 명확히 다른 방향으로
				- 한 추천은 conservative (안전한 정공법), 한 추천은 experimental (대담한 시도)
				- 마지막 한 추천은 위 둘의 중간 어딘가
				""";
		};
	}

	/**
	 * A.4: 브랜드 가이드 스냅샷을 시스템 프롬프트 뒤에 합성. brandGuide 가 null 이면 베이스 프롬프트 그대로 반환.
	 */
	public String compileRecommendationSystemPrompt(Style style,
		com.monovai.domain.business.brand.entity.BrandGuide brandGuide) {
		String base = compileRecommendationSystemPrompt(style);
		if (brandGuide == null) {
			return base;
		}
		StringBuilder sb = new StringBuilder(base);
		sb.append("\n\n[브랜드 가이드 — 다음 정체성을 반드시 반영]\n");
		sb.append("브랜드명: ").append(brandGuide.getName()).append("\n");
		if (brandGuide.getDescription() != null && !brandGuide.getDescription().isBlank()) {
			sb.append("설명: ").append(brandGuide.getDescription()).append("\n");
		}
		var identity = brandGuide.getIdentity();
		if (identity != null) {
			if (identity.colors() != null && !identity.colors().isEmpty()) {
				sb.append("컬러: ");
				identity.colors().forEach(c -> sb.append(c.hex())
					.append(c.description() != null ? "(" + c.description() + ")" : "").append(" "));
				sb.append("\n");
			}
			if (identity.typography() != null && !identity.typography().isEmpty()) {
				sb.append("타이포그래피: ");
				identity.typography().forEach(t -> sb.append(t.family()).append(" "));
				sb.append("\n");
			}
			if (identity.moods() != null && !identity.moods().isEmpty()) {
				sb.append("무드: ");
				identity.moods().forEach(m -> sb.append(m.label() != null ? m.label() + " " : ""));
				sb.append("\n");
			}
		}
		if (brandGuide.getPalette() != null && !brandGuide.getPalette().isEmpty()) {
			sb.append("팔레트: ");
			brandGuide.getPalette().forEach(p -> sb.append(p.role()).append("=").append(p.hex()).append(" "));
			sb.append("\n");
		}
		if (brandGuide.getGuidelines() != null && !brandGuide.getGuidelines().isEmpty()) {
			sb.append("가이드라인:\n");
			brandGuide.getGuidelines().forEach(g ->
				sb.append("- ").append(g.title()).append(": ")
					.append(g.summary() != null ? g.summary() : "").append("\n"));
		}
		sb.append("globalLock 의 background/surface/lighting/mood 를 브랜드 컬러/무드와 일관되게 작성할 것.\n");
		return sb.toString();
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
		String brief = buildPhase1Brief(job, variant);
		// §26: Phase 2 — GPT (gpt-4.1) 로 한 번 더 컴파일해서 Nanobanana 용 풍부한 프롬프트 생성.
		// GPT 호출 실패 시 brief 를 그대로 사용 (fallback).
		try {
			String compiled = openAiChatService.complete(PHASE2_SYSTEM_PROMPT, brief);
			if (compiled != null && !compiled.isBlank()) {
				return compiled.trim();
			}
		} catch (Exception e) {
			log.warn("[PromptCompile/Phase2] GPT 컴파일 실패, brief 폴백: {}", e.getMessage());
		}
		return brief;
	}

	private String buildPhase1Brief(ImageJob job, ImageJobVariant variant) {
		GlobalLock lock = variant.getGlobalLock();

		StringBuilder sb = new StringBuilder();
		sb.append("Premium product photography.\n");
		sb.append("Concept: ").append(variant.getRecommendationTitle()).append(".\n");
		if (variant.getRecommendationDescription() != null
			&& !variant.getRecommendationDescription().isBlank()) {
			sb.append("Concept detail: ").append(variant.getRecommendationDescription()).append("\n");
		}

		if (lock != null) {
			if (lock.background() != null) sb.append("Background: ").append(lock.background()).append(".\n");
			if (lock.surface() != null) sb.append("Surface: ").append(lock.surface()).append(".\n");
			if (lock.lighting() != null) sb.append("Lighting: ").append(lock.lighting()).append(".\n");
			if (lock.mood() != null) sb.append("Mood: ").append(lock.mood()).append(".\n");
		}

		if (job.getAngle() != null) {
			sb.append("Camera angle: ").append(job.getAngle().getValue()).append(".\n");
		}
		if (job.getLighting() != null) {
			sb.append("Lighting style preference: ").append(job.getLighting().getValue()).append(".\n");
		}
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
