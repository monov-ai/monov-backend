package com.monovai.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OnboardingRequest(
	@NotBlank(message = "job 은 필수입니다.") String job,
	@NotBlank(message = "source 는 필수입니다.") String source
) {
}
