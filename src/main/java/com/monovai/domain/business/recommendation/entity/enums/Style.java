package com.monovai.domain.business.recommendation.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Style {
	STUDIO,
	BANNER_EVENT,
	SOURCE_IMAGE,
	FREEFORM;

	@JsonValue
	public String getValue() {
		return name().toLowerCase().replace('_', '-');
	}

	@JsonCreator
	public static Style from(String value) {
		if (value == null) {
			return null;
		}
		return Style.valueOf(value.toUpperCase().replace('-', '_'));
	}

	public String getLabel() {
		return switch (this) {
			case STUDIO -> "스튜디오";
			case BANNER_EVENT -> "배너/이벤트";
			case SOURCE_IMAGE -> "소스 이미지";
			case FREEFORM -> "자유";
		};
	}
}
