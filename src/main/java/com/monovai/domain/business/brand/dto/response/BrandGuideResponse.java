package com.monovai.domain.business.brand.dto.response;

import java.time.Instant;
import java.util.List;

import com.monovai.domain.business.brand.entity.BrandGuide;
import com.monovai.domain.business.brand.entity.value.BrandAsset;
import com.monovai.domain.business.brand.entity.value.BrandGuideline;
import com.monovai.domain.business.brand.entity.value.BrandIdentity;
import com.monovai.domain.business.brand.entity.value.BrandPalette;
import com.monovai.domain.business.brand.entity.value.BrandUsage;

public record BrandGuideResponse(
	boolean ok,
	GuideView guide
) {
	public record GuideView(
		String id,
		Long userId,
		String name,
		boolean isDefault,
		String description,
		Instant createdAt,
		Instant updatedAt,
		BrandIdentity identity,
		List<BrandPalette> palette,
		List<BrandGuideline> guidelines,
		List<BrandAsset> assets,
		List<BrandUsage> recentUsages
	) {
		public static GuideView of(BrandGuide g) {
			return new GuideView(
				g.getGuideSlug(),
				g.getUser().getId(),
				g.getName(),
				g.isDefault(),
				g.getDescription(),
				g.getCreatedAt() != null ? g.getCreatedAt().toInstant() : null,
				g.getUpdatedAt() != null ? g.getUpdatedAt().toInstant() : null,
				g.getIdentity() != null ? g.getIdentity() : BrandIdentity.empty(),
				g.getPalette() != null ? g.getPalette() : List.of(),
				g.getGuidelines() != null ? g.getGuidelines() : List.of(),
				g.getAssets() != null ? g.getAssets() : List.of(),
				g.getRecentUsages() != null ? g.getRecentUsages() : List.of()
			);
		}
	}

	public static BrandGuideResponse of(BrandGuide g) {
		return new BrandGuideResponse(true, GuideView.of(g));
	}
}
