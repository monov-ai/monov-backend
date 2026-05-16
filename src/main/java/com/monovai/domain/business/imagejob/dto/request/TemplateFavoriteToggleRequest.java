package com.monovai.domain.business.imagejob.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateFavoriteToggleRequest(
	@NotBlank(message = "itemId 는 필수입니다.") String itemId,
	@NotBlank(message = "mediaType 은 필수입니다.") String mediaType,
	@NotNull(message = "favorite 는 필수입니다.") Boolean favorite
) {
}
