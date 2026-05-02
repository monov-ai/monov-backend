package com.monovai.domain.business.imagejob.entity.enums;

public enum JobStatus {
	PENDING,
	RUNNING,
	PENDING_IMAGES,
	COMPLETED,
	PARTIAL,
	FAILED;

	public String getValue() {
		return name().toLowerCase();
	}

	public boolean isTerminal() {
		return this == COMPLETED || this == PARTIAL || this == FAILED;
	}
}