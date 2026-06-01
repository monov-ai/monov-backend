package com.monovai.domain.business.imagejob.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * v2.0: angle 제거 (필수 아님). recommendationIds 다중, recommendationId 단수형 호환.
 */
public record GenerateImageRequest(
	@NotBlank(message = "requestId 는 필수입니다.")
	String requestId,

	List<String> recommendationIds,

	String recommendationId, // v1 호환

	String angle,            // v2.0 옵셔널 (deprecated)

	@NotBlank(message = "lighting 은 필수입니다.")
	String lighting,

	@NotBlank(message = "ratio 는 필수입니다.")
	String ratio,

	// §18/§29: SOURCE_IMAGE 스타일에서 true 면 OpenAI gpt-image-1 (배경 투명),
	// false 면 gpt-image-2. FREEFORM/STUDIO/BANNER_EVENT 에서는 무시.
	Boolean transparentBackground
) {

	public List<String> normalizedRecommendationIds() {
		List<String> out = new ArrayList<>();
		if (recommendationIds != null) {
			for (String r : recommendationIds) if (r != null && !r.isBlank()) out.add(r);
		}
		if (out.isEmpty() && recommendationId != null && !recommendationId.isBlank()) {
			out.add(recommendationId);
		}
		return out;
	}
}
