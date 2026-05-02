package com.monovai.domain.business.imagejob.entity.enums;

public enum VariantStatus {
	PENDING,
	RUNNING,
	PENDING_IMAGES,
	COMPLETED,
	FAILED;

	public String getValue() {
		return name().toLowerCase();
	}
}