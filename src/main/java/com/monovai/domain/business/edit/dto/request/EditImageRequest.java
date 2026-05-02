package com.monovai.domain.business.edit.dto.request;

import com.monovai.domain.business.edit.entity.value.EditParams;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditImageRequest(
	@NotBlank(message = "jobId 는 필수입니다.")
	String jobId,

	@NotBlank(message = "baseId 는 필수입니다.")
	String baseId,

	@NotBlank(message = "mode 는 필수입니다.")
	String mode,

	@NotNull(message = "params 는 필수입니다.")
	EditParams params
) {
}
