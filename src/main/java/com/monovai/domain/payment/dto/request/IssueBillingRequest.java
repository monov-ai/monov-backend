package com.monovai.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record IssueBillingRequest(
	@NotBlank String authKey,
	@NotBlank String planId
) {
}
