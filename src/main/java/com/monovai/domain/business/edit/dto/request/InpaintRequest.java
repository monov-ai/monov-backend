package com.monovai.domain.business.edit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InpaintRequest(
	@NotBlank(message = "jobId 는 필수입니다.")
	String jobId,

	String baseId,
	String baseVariantId,

	@NotBlank(message = "prompt 는 필수입니다.")
	@Size(min = 1, max = 2000, message = "prompt 는 1~2000자 사이여야 합니다.")
	String prompt,

	@NotBlank(message = "maskBase64 는 필수입니다.")
	String maskBase64,

	@NotNull(message = "maskWidth 는 필수입니다.")
	Integer maskWidth,

	@NotNull(message = "maskHeight 는 필수입니다.")
	Integer maskHeight,

	String size,

	Boolean transparentBackground
) {
	public String resolvedBaseId() {
		if (baseId != null && !baseId.isBlank()) return baseId;
		return baseVariantId;
	}
}
