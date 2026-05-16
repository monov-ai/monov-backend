package com.monovai.domain.business.imagejob.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoriteToggleRequest(
	@NotBlank(message = "jobId 는 필수입니다.") String jobId,
	@NotBlank(message = "kind 는 필수입니다.") String kind,
	@NotBlank(message = "id 는 필수입니다.") String id,
	@NotNull(message = "favorite 는 필수입니다.") Boolean favorite
) {
}
