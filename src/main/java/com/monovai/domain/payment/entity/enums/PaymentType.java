package com.monovai.domain.payment.entity.enums;

public enum PaymentType {
	ONE_TIME, SUBSCRIPTION, REFUND;

	public String getValue() {
		return name().toLowerCase();
	}
}
