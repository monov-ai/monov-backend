package com.monovai.domain.business.brand.entity.value;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrandIdentity(
	List<Logo> logos,
	List<Color> colors,
	List<Typography> typography,
	List<Mood> moods
) {
	public static BrandIdentity empty() {
		return new BrandIdentity(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

	public record Logo(String id, String name, String imageUrl, String storagePath, String background) {
	}

	public record Color(String id, String hex, String description) {
	}

	public record Typography(String id, String family, String role, List<String> weights, Boolean isCustom) {
	}

	public record Mood(String id, String imageUrl, String storagePath, String label) {
	}
}
