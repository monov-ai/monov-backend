package com.monovai.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(
	@NotBlank String paymentKey,
	@NotBlank String orderId,
	@NotNull Integer amount,
	String packageId,
	String planId
) {
}
