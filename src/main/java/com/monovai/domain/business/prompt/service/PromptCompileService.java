package com.monovai.domain.business.prompt.service;

import org.springframework.stereotype.Service;

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

	/**
	 * Phase 1: 사용자 입력 + 입력 이미지로 GPT 호출에 사용할 시스템 프롬프트 생성.
	 */
	public String compileRecommendationSystemPrompt(String inputJson, String inputImageUrl) {
		// TODO: banner-event / source-image 분기 + 시스템 프롬프트 합성
		throw new UnsupportedOperationException("compileRecommendationSystemPrompt not implemented");
	}

	/**
	 * Phase 2: 선택된 추천 + 옵션으로 Nanobanana 이미지 생성 prompt 합성.
	 */
	public String compileImageGenerationPrompt(String compiledRecommendationPrompt, String optionsJson) {
		// TODO
		throw new UnsupportedOperationException("compileImageGenerationPrompt not implemented");
	}

	/**
	 * Phase 3: 빠른 수정 prompt 합성 (체이닝된 base 이미지 + edit type + 사용자 prompt).
	 */
	public String compileEditPrompt(String baseImageUrl, String editType, String userPrompt) {
		// TODO: GPT compile (v1.1 신규)
		throw new UnsupportedOperationException("compileEditPrompt not implemented");
	}
}