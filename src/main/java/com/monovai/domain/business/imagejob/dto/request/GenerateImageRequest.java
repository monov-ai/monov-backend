package com.monovai.domain.business.imagejob.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record GenerateImageRequest(
	@NotBlank(message = "requestId 는 필수입니다.")
	String requestId,

	@NotEmpty(message = "1개 이상의 recommendationId 가 필요합니다.")
	List<@NotBlank String> recommendationIds,

	@NotBlank(message = "angle 은 필수입니다.")
	String angle,

	@NotBlank(message = "lighting 은 필수입니다.")
	String lighting,

	@NotBlank(message = "ratio 는 필수입니다.")
	String ratio
) {
}