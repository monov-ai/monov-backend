package com.monovai.domain.payment.entity.enums;

public enum PromotionStatus {
	ACTIVE, REDEEMED;

	public String getValue() {
		return name().toLowerCase();
	}
}
