package com.monovai.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record ConsentRequest(
	@NotNull(message = "termsAccepted 는 필수입니다.") Boolean termsAccepted,
	@NotNull(message = "privacyAccepted 는 필수입니다.") Boolean privacyAccepted,
	@NotNull(message = "contentUsageAccepted 는 필수입니다.") Boolean contentUsageAccepted,
	Boolean marketingAccepted,
	String version
) {
}
