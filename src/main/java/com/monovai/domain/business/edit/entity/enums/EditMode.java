package com.monovai.domain.business.edit.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

/**
 * v2.0: TEXT_CREATE (자유 텍스트로 새 이미지 생성) 와 INPAINT (마스크 기반 편집, OpenAI gpt-image-1) 추가.
 * ANGLE_CHANGE 는 v2.0 에서 rotation/tilt 좌표 기반 (legacy enum 도 fallback 지원).
 */
public enum EditMode {
	BACKGROUND_CHANGE,
	LIGHTING_CHANGE,
	ANGLE_CHANGE,
	RATIO_CHANGE,
	PRODUCT_REPLACE,
	OBJECT_ADD,
	TEXT_CREATE,
	INPAINT;

	public String getValue() {
		return name().toLowerCase();
	}

	public static EditMode from(String value) {
		if (value == null) {
			throw new BadRequestException(ErrorCode.INVALID_EDIT_MODE);
		}
		try {
			return EditMode.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(ErrorCode.INVALID_EDIT_MODE);
		}
	}
}