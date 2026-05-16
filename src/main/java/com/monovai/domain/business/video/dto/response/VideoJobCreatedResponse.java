package com.monovai.domain.business.video.dto.response;

public record VideoJobCreatedResponse(String videoId) {
	public static VideoJobCreatedResponse of(String videoId) {
		return new VideoJobCreatedResponse(videoId);
	}
}
