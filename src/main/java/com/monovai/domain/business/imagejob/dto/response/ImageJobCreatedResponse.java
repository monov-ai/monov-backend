package com.monovai.domain.business.imagejob.dto.response;

public record ImageJobCreatedResponse(String jobId) {

	public static ImageJobCreatedResponse of(String jobId) {
		return new ImageJobCreatedResponse(jobId);
	}
}