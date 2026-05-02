package com.monovai.domain.business.edit.entity.value;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * mode 별로 들어가는 params. mode 마다 채워지는 필드가 다름.
 * - BACKGROUND_CHANGE: description / referenceImageUrl / referenceImagePath (둘 중 1개+)
 * - LIGHTING_CHANGE  : lighting (필수)
 * - ANGLE_CHANGE     : angle (필수)
 * - RATIO_CHANGE     : ratio (필수)
 * - PRODUCT_REPLACE  : referenceImageUrl / referenceImagePath (1개+)
 * - OBJECT_ADD       : description / referenceImageUrl / referenceImagePath (1개+)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EditParams(
	String description,
	String referenceImageUrl,
	String referenceImagePath,
	String lighting,
	String angle,
	String ratio
) {
	public boolean hasDescription() {
		return description != null && !description.isBlank();
	}

	public boolean hasReferenceImage() {
		return (referenceImageUrl != null && !referenceImageUrl.isBlank())
			|| (referenceImagePath != null && !referenceImagePath.isBlank());
	}
}