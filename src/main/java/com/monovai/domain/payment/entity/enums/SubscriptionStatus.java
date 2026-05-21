package com.monovai.domain.payment.entity.enums;

public enum SubscriptionStatus {
	ACTIVE, CANCELLED, PAST_DUE, PENDING;

	public String getValue() {
		return name().toLowerCase();
	}
}
