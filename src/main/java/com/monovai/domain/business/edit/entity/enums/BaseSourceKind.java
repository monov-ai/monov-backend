package com.monovai.domain.business.edit.entity.enums;

/**
 * Edit 의 base 가 어떤 종류인지.
 * - VARIANT: 부모 ImageJob 의 variants[].variantId (예: "V1") 를 참조
 * - EDIT: 같은 jobId 의 다른 ImageEdit (체이닝) 을 참조
 */
public enum BaseSourceKind {
	VARIANT,
	EDIT;

	public String getValue() {
		return name().toLowerCase();
	}
}