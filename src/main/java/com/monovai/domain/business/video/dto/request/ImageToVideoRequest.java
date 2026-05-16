package com.monovai.domain.business.video.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ImageToVideoRequest(
	@NotBlank(message = "imageUrl 은 필수입니다.")
	String imageUrl,

	String imagePath,
	String ratio,           // 9:16 | 16:9 | 1:1, default 9:16
	String userPrompt,
	String sourceJobId,
	String sourceItemId,
	String sourceKind       // variant | edit
) {
}
