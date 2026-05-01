package com.monovai.domain.business.recommendation.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

public enum Style {
	STUDIO,
	BANNER_EVENT,
	SOURCE_IMAGE,
	FREEFORM;

	/**
	 * spec wire format ("studio", "banner-event") 으로 직렬화할 때 사용.
	 */
	public String getValue() {
		return name().toLowerCase().replace('_', '-');
	}

	/**
	 * UI 표시용 한국어 라벨.
	 */
	public String getLabel() {
		return switch (this) {
			case STUDIO -> "스튜디오";
			case BANNER_EVENT -> "배너/이벤트";
			case SOURCE_IMAGE -> "소스 이미지";
			case FREEFORM -> "자유";
		};
	}

	/**
	 * spec wire format 문자열을 Style 로 변환. 잘못된 값이면 BadRequestException.
	 * 컨트롤러 단계가 아니라 서비스 레이어에서 명시적으로 호출하기 위함.
	 */
	public static Style from(String value) {
		if (value == null) {
			throw new BadRequestException(ErrorCode.INVALID_STYLE);
		}
		try {
			return Style.valueOf(value.toUpperCase().replace('-', '_'));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(ErrorCode.INVALID_STYLE);
		}
	}
}