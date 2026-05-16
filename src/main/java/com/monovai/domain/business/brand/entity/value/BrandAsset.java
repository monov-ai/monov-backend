package com.monovai.domain.business.brand.entity.value;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrandAsset(
	String id,
	String name,
	String fileName,
	String fileType,
	String imageUrl,
	String storagePath,
	Long sizeBytes
) {
}
