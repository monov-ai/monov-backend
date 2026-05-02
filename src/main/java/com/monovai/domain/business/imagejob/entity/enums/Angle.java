package com.monovai.domain.business.imagejob.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

public enum Angle {
	FRONT,
	ANGLE_45,
	SIDE,
	TOP;

	public String getValue() {
		return switch (this) {
			case FRONT -> "front";
			case ANGLE_45 -> "45";
			case SIDE -> "side";
			case TOP -> "top";
		};
	}

	public static Angle from(String value) {
		if (value == null) {
			throw new BadRequestException(ErrorCode.INVALID_ANGLE);
		}
		return switch (value) {
			case "front" -> FRONT;
			case "45" -> ANGLE_45;
			case "side" -> SIDE;
			case "top" -> TOP;
			default -> throw new BadRequestException(ErrorCode.INVALID_ANGLE);
		};
	}
}