package com.monovai.domain.business.imagejob.dto.response;

public record ImageJobCreatedResponse(Long jobId) {

	public static ImageJobCreatedResponse of(Long jobId) {
		return new ImageJobCreatedResponse(jobId);
	}
}