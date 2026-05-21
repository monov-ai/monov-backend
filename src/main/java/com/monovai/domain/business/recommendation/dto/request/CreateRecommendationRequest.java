package com.monovai.domain.business.recommendation.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * v2.0: 슬롯당 ≤4장 다중 이미지를 지원. 단수 형도 v1 호환용으로 받음.
 * 어느 쪽이든 들어오면 normalize 로 List 로 통일.
 */
public record CreateRecommendationRequest(
	@NotBlank(message = "스타일은 필수입니다.")
	String style,

	@Size(max = 1000, message = "설명은 최대 1000자까지 입력 가능합니다.")
	String description,

	// v2.0 다중 (≤4)
	List<String> productImageUrls,
	List<String> productImagePaths,
	List<String> referenceImageUrls,
	List<String> referenceImagePaths,

	// v1 호환 단수형
	String productImageUrl,
	String productImagePath,
	String referenceImageUrl,
	String referenceImagePath,

	// A.4: 브랜드 가이드 적용 (선택)
	String brandKitId
) {

	public List<String> normalizedProductImageUrls() {
		return merge(productImageUrls, productImageUrl);
	}

	public List<String> normalizedProductImagePaths() {
		return merge(productImagePaths, productImagePath);
	}

	public List<String> normalizedReferenceImageUrls() {
		return merge(referenceImageUrls, referenceImageUrl);
	}

	public List<String> normalizedReferenceImagePaths() {
		return merge(referenceImagePaths, referenceImagePath);
	}

	private static List<String> merge(List<String> list, String singular) {
		List<String> out = new ArrayList<>();
		if (list != null) {
			for (String s : list) if (s != null && !s.isBlank()) out.add(s);
		}
		if (out.isEmpty() && singular != null && !singular.isBlank()) {
			out.add(singular);
		}
		return out;
	}
}
