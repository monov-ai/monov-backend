package com.monovai.domain.business.imagejob.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

public enum Ratio {
	ONE_TO_ONE,
	NINE_TO_SIXTEEN,
	FOUR_TO_THREE;

	public String getValue() {
		return switch (this) {
			case ONE_TO_ONE -> "1:1";
			case NINE_TO_SIXTEEN -> "9:16";
			case FOUR_TO_THREE -> "4:3";
		};
	}

	public static Ratio from(String value) {
		if (value == null) {
			throw new BadRequestException(ErrorCode.INVALID_RATIO);
		}
		return switch (value) {
			case "1:1" -> ONE_TO_ONE;
			case "9:16" -> NINE_TO_SIXTEEN;
			case "4:3" -> FOUR_TO_THREE;
			default -> throw new BadRequestException(ErrorCode.INVALID_RATIO);
		};
	}
}