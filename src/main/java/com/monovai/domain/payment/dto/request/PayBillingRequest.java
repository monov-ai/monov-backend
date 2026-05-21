package com.monovai.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PayBillingRequest(
	@NotBlank String billingKey,
	@NotBlank String planId
) {
}
