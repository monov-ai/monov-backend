package com.monovai.domain.business.video.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

public enum MediaType {
	IMAGE,
	VIDEO;

	public String getValue() {
		return name().toLowerCase();
	}

	public static MediaType from(String value) {
		if (value == null) throw new BadRequestException(ErrorCode.INVALID_MEDIA_TYPE);
		return switch (value.toLowerCase()) {
			case "image" -> IMAGE;
			case "video" -> VIDEO;
			default -> throw new BadRequestException(ErrorCode.INVALID_MEDIA_TYPE);
		};
	}
}
