package com.monovai.domain.upload.dto.response;

import java.time.Instant;

public record PresignedUrlResponse(
	String url,
	String key,
	Instant expiresAt
) {
	public static PresignedUrlResponse of(String url, String key, Instant expiresAt) {
		return new PresignedUrlResponse(url, key, expiresAt);
	}
}