package com.monovai.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePromotionRequest(
	@NotNull @Positive Integer credits,
	Long issuedTo,
	Integer expiresInDays
) {
}
