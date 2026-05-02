package com.monovai.domain.upload.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.upload.dto.request.IssueUploadUrlRequest;
import com.monovai.domain.upload.dto.response.PresignedUrlResponse;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.ForbiddenException;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 업로드 자체는 프론트가 S3 에 직접 PUT — 백엔드는 presigned URL 만 발급.
 * - issueUploadUrl: 프론트가 곧장 PUT 할 수 있는 10분짜리 PUT presigned URL + 자동 생성된 key
 * - getDownloadUrl: 기존 key 에 대한 7일짜리 GET presigned URL (재서명)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UploadService {

	private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
	private static final Duration DOWNLOAD_TTL = Duration.ofDays(7);

	private final S3Service s3Service;

	/**
	 * 업로드용 PUT presigned URL 발급. 프론트가 그 URL 로 S3 에 직접 PUT.
	 */
	public PresignedUrlResponse issueUploadUrl(Long userId, IssueUploadUrlRequest request) {
		String safeFilename = sanitize(request.fileName());
		long timestamp = System.currentTimeMillis() / 1000;
		String key = "user_uploads/" + userId + "/template_requests/" + timestamp + "_" + safeFilename;

		String url = s3Service.getPreSignedUrlForUpload(key);
		Instant expiresAt = Instant.now().plus(UPLOAD_TTL);

		log.info("[Upload] issueUploadUrl userId={}, key={}, fileName={}", userId, key, request.fileName());
		return PresignedUrlResponse.of(url, key, expiresAt);
	}

	/**
	 * 기존 S3 key 의 다운로드 presigned URL 재발급.
	 * 권한: user_uploads/{userId}/ 본인 소유, business_images/ 는 모두 OK.
	 */
	public PresignedUrlResponse getDownloadUrl(Long userId, String key) {
		authorizeKeyAccess(userId, key);
		String url = s3Service.getPreSignedUrlForDownload(key, DOWNLOAD_TTL);
		Instant expiresAt = Instant.now().plus(DOWNLOAD_TTL);
		return PresignedUrlResponse.of(url, key, expiresAt);
	}

	private void authorizeKeyAccess(Long userId, String key) {
		if (key == null || key.isBlank()) {
			throw new BadRequestException(ErrorCode.MISSING_PARAMETER);
		}
		if (key.startsWith("business_images/")) {
			return;
		}
		String ownPrefix = "user_uploads/" + userId + "/";
		if (!key.startsWith(ownPrefix)) {
			throw new ForbiddenException(ErrorCode.S3_KEY_FORBIDDEN);
		}
	}

	private String sanitize(String filename) {
		if (filename == null || filename.isBlank()) {
			return "image";
		}
		String cleaned = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (cleaned.length() > 80) {
			cleaned = cleaned.substring(cleaned.length() - 80);
		}
		return cleaned;
	}
}