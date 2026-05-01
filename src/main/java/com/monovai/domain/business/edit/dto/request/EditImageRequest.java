package com.monovai.domain.business.edit.dto.request;

import com.monovai.domain.business.edit.entity.enums.EditType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditImageRequest(
	@NotNull(message = "기준 ID 는 필수입니다.")
	Long baseId,

	@NotNull(message = "수정 타입은 필수입니다.")
	EditType editType,

	@NotBlank(message = "프롬프트는 필수입니다.")
	String prompt
) {
}