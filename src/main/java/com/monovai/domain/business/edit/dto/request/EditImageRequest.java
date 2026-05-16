package com.monovai.domain.business.edit.dto.request;

import com.monovai.domain.business.edit.entity.value.EditParams;

import jakarta.validation.constraints.NotBlank;

/**
 * v2.0: baseId 또는 baseVariantId(legacy) 어느 쪽이라도 받음.
 * params 는 mode 별로 필드가 달라서 @NotNull 만 (내부 validate 는 service 에서).
 */
public record EditImageRequest(
	@NotBlank(message = "jobId 는 필수입니다.")
	String jobId,

	String baseId,
	String baseVariantId,

	@NotBlank(message = "mode 는 필수입니다.")
	String mode,

	EditParams params
) {

	public String resolvedBaseId() {
		if (baseId != null && !baseId.isBlank()) return baseId;
		return baseVariantId;
	}
}
