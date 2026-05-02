package com.monovai.domain.business.edit.entity.enums;

public enum EditStatus {
	PENDING,
	RUNNING,
	PENDING_IMAGES,
	COMPLETED,
	FAILED;

	public String getValue() {
		return name().toLowerCase();
	}

	public boolean isTerminal() {
		return this == COMPLETED || this == FAILED;
	}
}