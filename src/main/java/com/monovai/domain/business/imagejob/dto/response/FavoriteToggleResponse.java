package com.monovai.domain.business.imagejob.dto.response;

import java.util.List;

public record FavoriteToggleResponse(
	boolean ok,
	List<String> favoriteVariantIds,
	List<String> favoriteEditIds
) {
	public static FavoriteToggleResponse of(List<String> v, List<String> e) {
		return new FavoriteToggleResponse(true, v, e);
	}
}
