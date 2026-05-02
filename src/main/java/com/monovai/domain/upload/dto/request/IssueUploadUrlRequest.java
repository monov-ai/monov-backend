package com.monovai.domain.upload.dto.request;

import jakarta.validation.constraints.NotBlank;

public record IssueUploadUrlRequest(
	@NotBlank(message = "파일 이름은 필수입니다.")
	String fileName,

	String contentType
) {
}