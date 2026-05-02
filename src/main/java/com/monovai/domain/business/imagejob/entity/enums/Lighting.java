package com.monovai.domain.business.imagejob.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

public enum Lighting {
	NATURAL,
	WARM,
	SOFT,
	STRONG;

	public String getValue() {
		return name().toLowerCase();
	}

	public static Lighting from(String value) {
		if (value == null) {
			throw new BadRequestException(ErrorCode.INVALID_LIGHTING);
		}
		try {
			return Lighting.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(ErrorCode.INVALID_LIGHTING);
		}
	}
}