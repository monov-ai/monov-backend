package com.monovai.domain.business.imagejob.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

public enum Ratio {
	ONE_TO_ONE,
	NINE_TO_SIXTEEN,
	FOUR_TO_THREE,
	THREE_TO_FOUR,
	SIXTEEN_TO_NINE;

	public String getValue() {
		return switch (this) {
			case ONE_TO_ONE -> "1:1";
			case NINE_TO_SIXTEEN -> "9:16";
			case FOUR_TO_THREE -> "4:3";
			case THREE_TO_FOUR -> "3:4";
			case SIXTEEN_TO_NINE -> "16:9";
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
			case "3:4" -> THREE_TO_FOUR;
			case "16:9" -> SIXTEEN_TO_NINE;
			default -> throw new BadRequestException(ErrorCode.INVALID_RATIO);
		};
	}
}