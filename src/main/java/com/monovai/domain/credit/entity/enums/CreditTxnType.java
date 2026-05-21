package com.monovai.domain.credit.entity.enums;

public enum CreditTxnType {
	DEDUCT,
	REFUND,
	GRANT_PROMO,
	GRANT_SUBSCRIPTION,
	GRANT_SIGNUP,
	GRANT_ONE_TIME,
	ADJUST;

	public String getValue() {
		return name().toLowerCase();
	}
}
