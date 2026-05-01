package com.monovai.external.openai.dto;

import java.util.List;

import com.monovai.domain.business.recommendation.entity.value.CorePoints;
import com.monovai.domain.business.recommendation.entity.value.RecommendationItem;

/**
 * Spring AI structured output 으로 GPT 응답을 매핑할 record.
 *
 * 도메인 value object (CorePoints, RecommendationItem, GlobalLock) 를 그대로 재사용 — GPT 응답이
 * 도메인 모양과 1:1 이라 변환 코드 0. external → domain.value 의존은 value object 가 행위 없는
 * 안정 계약이라 허용.
 */
public record GptRecommendationResponse(
	String headline,
	String summary,
	CorePoints corePoints,
	List<RecommendationItem> recommendations
) {
}
