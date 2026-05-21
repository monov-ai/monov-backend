package com.monovai.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LocaleRequest(
	@NotBlank(message = "locale 은 필수입니다.") String locale
) {
}
