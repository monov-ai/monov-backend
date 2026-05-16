package com.monovai.domain.business.brand.dto.response;

public record BrandUploadResponse(
	boolean ok,
	String id,
	String url,
	String storagePath,
	String fileName,
	String fileType,
	long sizeBytes,
	String kind
) {
}
