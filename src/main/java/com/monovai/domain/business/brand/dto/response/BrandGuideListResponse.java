package com.monovai.domain.business.brand.dto.response;

import java.util.List;

public record BrandGuideListResponse(
	boolean ok,
	List<BrandGuideResponse.GuideView> guides
) {
	public static BrandGuideListResponse of(List<BrandGuideResponse.GuideView> guides) {
		return new BrandGuideListResponse(true, guides);
	}
}
