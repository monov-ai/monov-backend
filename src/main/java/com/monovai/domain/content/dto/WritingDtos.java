package com.monovai.domain.content.dto;

import java.util.List;

/**
 * C.2 Writing 입출력 계약. monov-functions 의 기존 contract 와 동일.
 */
public final class WritingDtos {

	private WritingDtos() {
	}

	public record SuggestTopicsRequest(String category, String description) {
	}

	public record Topic(String title, String summary, List<String> tags) {
	}

	public record SuggestTopicsResponse(List<Topic> topics) {
	}

	public record GenerateRequest(String category, String topic, String tone, String description) {
	}

	public record Post(String title, String body, List<String> hashtags) {
	}

	public record GenerateResponse(Post post) {
	}

	public record RefineRequest(Post post, String instruction) {
	}

	public record RefineResponse(Post post) {
	}
}
