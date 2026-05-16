package com.monovai.domain.business.video.entity.enums;

public enum VideoStatus {
	REQUESTED,
	RUNNING,
	COMPLETED,
	FAILED;

	public String getValue() {
		return name().toLowerCase();
	}
}
