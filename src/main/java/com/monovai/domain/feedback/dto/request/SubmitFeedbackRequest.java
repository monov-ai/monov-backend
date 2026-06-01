package com.monovai.domain.feedback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SubmitFeedbackRequest(
	@Size(max = 40)
	String type,

	@Size(max = 20)
	String mediaType,

	@Size(max = 100)
	String templateId,

	@Size(max = 200)
	String templateTitle,

	@Size(max = 100)
	String itemId,

	@Size(max = 20)
	String downloadSource,

	@Min(value = 1, message = "rating 은 1~5 사이여야 합니다.")
	@Max(value = 5, message = "rating 은 1~5 사이여야 합니다.")
	int rating,

	@Size(max = 100)
	String tag,

	@Size(max = 300, message = "note 는 최대 300자입니다.")
	String note
) {
}
