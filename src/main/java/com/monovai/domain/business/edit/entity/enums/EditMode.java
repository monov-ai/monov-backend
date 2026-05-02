package com.monovai.domain.business.edit.entity.enums;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;

/**
 * v1.1: BACKGROUND_CHANGE 도 enum 으로는 유지하되, params 가 자유 텍스트 + 레퍼런스 이미지로 바뀜.
 * INPAINT 는 spec 에서 미구현 — 거부 처리.
 */
public enum EditMode {
	BACKGROUND_CHANGE,
	LIGHTING_CHANGE,
	ANGLE_CHANGE,
	RATIO_CHANGE,
	PRODUCT_REPLACE,
	OBJECT_ADD;

	public String getValue() {
		return name().toLowerCase();
	}

	public static EditMode from(String value) {
		if (value == null) {
			throw new BadRequestException(ErrorCode.INVALID_EDIT_MODE);
		}
		String v = value.toLowerCase();
		if ("inpaint".equals(v)) {
			throw new BadRequestException(ErrorCode.EDIT_MODE_INPAINT_NOT_SUPPORTED);
		}
		try {
			return EditMode.valueOf(v.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(ErrorCode.INVALID_EDIT_MODE);
		}
	}
}