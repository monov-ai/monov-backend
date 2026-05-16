package com.monovai.domain.business.brand.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBrandGuideRequest(
	@NotBlank(message = "브랜드 이름은 필수입니다.") String name,
	String description,
	Boolean isDefault
) {
}
