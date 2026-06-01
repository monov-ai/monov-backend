package com.monovai.domain.business.imagejob.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * §15: 사용자가 업로드한 이미지 1장을 V1=완료 상태로 등록.
 * imagePath 의 S3 key prefix 가 호출 사용자 ID 와 일치해야 함 (서비스에서 검증).
 */
public record CreateJobFromUploadRequest(
	@NotBlank(message = "imageUrl 은 필수입니다.")
	String imageUrl,

	@NotBlank(message = "imagePath (S3 key) 는 필수입니다.")
	String imagePath,

	@NotBlank(message = "ratio 는 필수입니다.")
	String ratio,

	@Size(max = 60, message = "title 은 최대 60자입니다.")
	String title
) {
}
