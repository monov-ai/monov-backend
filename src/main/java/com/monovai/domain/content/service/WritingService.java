package com.monovai.domain.content.service;

import org.springframework.stereotype.Service;

import com.monovai.domain.content.dto.WritingDtos.GenerateRequest;
import com.monovai.domain.content.dto.WritingDtos.GenerateResponse;
import com.monovai.domain.content.dto.WritingDtos.RefineRequest;
import com.monovai.domain.content.dto.WritingDtos.RefineResponse;
import com.monovai.domain.content.dto.WritingDtos.SuggestTopicsRequest;
import com.monovai.domain.content.dto.WritingDtos.SuggestTopicsResponse;
import com.monovai.external.openai.service.OpenAiChatService;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WritingService {

	private final OpenAiChatService openAiChatService;

	public SuggestTopicsResponse suggestTopics(SuggestTopicsRequest req) {
		String system = """
			너는 콘텐츠 기획자다. 주어진 카테고리/설명에 맞는 블로그/SNS 글감 5개를 추천한다.
			각 토픽은 title(한국어), summary(1~2문장), tags(3~5개) 를 포함한다.
			""";
		String user = "카테고리: " + nz(req.category()) + "\n설명: " + nz(req.description());
		return call(system, user, SuggestTopicsResponse.class);
	}

	public GenerateResponse generate(GenerateRequest req) {
		String system = """
			너는 전문 카피라이터다. 주어진 토픽으로 완성된 글 1편을 작성한다.
			출력은 post { title, body, hashtags(5~10개) } 형식.
			tone 이 주어지면 그 톤을 반영한다. 본문은 한국어, 마크다운 허용.
			""";
		String user = "카테고리: " + nz(req.category()) + "\n토픽: " + nz(req.topic())
			+ "\n톤: " + nz(req.tone()) + "\n추가설명: " + nz(req.description());
		return call(system, user, GenerateResponse.class);
	}

	public RefineResponse refine(RefineRequest req) {
		String system = """
			너는 교정/리라이팅 전문가다. 주어진 post 를 instruction 에 맞게 다듬어 동일한 post 구조로 반환한다.
			""";
		String user = "원본 post: " + (req.post() != null ? req.post().toString() : "")
			+ "\n지시사항: " + nz(req.instruction());
		return call(system, user, RefineResponse.class);
	}

	private <T> T call(String system, String user, Class<T> type) {
		try {
			return openAiChatService.structured(system, user, type);
		} catch (Exception e) {
			log.error("[Writing] AI 호출 실패", e);
			throw new BusinessException(ErrorCode.AI_CONTENT_FAILED);
		}
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
