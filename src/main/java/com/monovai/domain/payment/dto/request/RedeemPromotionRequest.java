package com.monovai.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RedeemPromotionRequest(
	@NotBlank String code
) {
}
