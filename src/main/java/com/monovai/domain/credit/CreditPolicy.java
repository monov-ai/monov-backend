package com.monovai.domain.credit;

/**
 * 크레딧 단가표 (기존 monov-web 정책).
 */
public final class CreditPolicy {

	private CreditPolicy() {
	}

	public static final int IMAGE_PER_UNIT = 20;       // 이미지 1장
	public static final int VIDEO_PER_UNIT = 80;       // 영상 1개
	public static final int INPAINT = 20;              // 인페인팅 1장
	public static final int EDIT_IMAGE = 20;           // edit-image 1장
	public static final int STORYBOARD_PREPARE = 80;
	public static final int BRAND_IMAGE_4 = 80;        // 4장 묶음
	public static final int SIGNUP_BONUS = 80;         // 가입 보너스 (이미지 4 또는 영상 1)

	public static int generateImage(int variantCount) {
		return Math.max(variantCount, 1) * IMAGE_PER_UNIT;
	}
}
