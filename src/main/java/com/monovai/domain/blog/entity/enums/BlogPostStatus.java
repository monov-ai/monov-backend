package com.monovai.domain.blog.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

public enum BlogPostStatus {
	DRAFT,
	PUBLISHED,
	ARCHIVED;

	public String getValue() {
		return name().toLowerCase();
	}

	public static BlogPostStatus from(String value) {
		if (value == null) return DRAFT;
		try {
			return BlogPostStatus.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(ErrorCode.BAD_REQUEST_DATA);
		}
	}
}
