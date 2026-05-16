package com.monovai.domain.business.brand.dto.request;

import java.util.List;

import com.monovai.domain.business.brand.entity.value.BrandAsset;
import com.monovai.domain.business.brand.entity.value.BrandGuideline;
import com.monovai.domain.business.brand.entity.value.BrandIdentity;
import com.monovai.domain.business.brand.entity.value.BrandPalette;
import com.monovai.domain.business.brand.entity.value.BrandUsage;

/**
 * Partial update — null 인 필드는 무시. 보호 필드 (id, userId, createdAt) 는 받지 않음.
 */
public record UpdateBrandGuideRequest(
	String name,
	String description,
	Boolean isDefault,
	BrandIdentity identity,
	List<BrandPalette> palette,
	List<BrandGuideline> guidelines,
	List<BrandAsset> assets,
	List<BrandUsage> recentUsages
) {
}
