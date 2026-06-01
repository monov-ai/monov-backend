package com.monovai.domain.business.edit.entity.value;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * mode 별 params. v2.0 변경 사항:
 * - 다중 이미지: referenceImageUrls / referenceImagePaths (≤4)
 * - angle_change : rotation(-180..180), tilt(-90..90) 좌표 추가. 옛 enum (front/45/side/top) fallback 도 받음.
 * - text_create  : description 자유 텍스트만
 * - inpaint      : prompt + maskUrl/maskPath + maskWidth/maskHeight + size
 *
 * 모든 필드 nullable. 직렬화 시 null 은 제외.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EditParams(
	// 공통 + background_change / object_add 텍스트 필드
	String description,

	// 단수형 (v1 호환)
	String referenceImageUrl,
	String referenceImagePath,

	// 다중 이미지 (v2.0, 슬롯당 ≤4)
	List<String> referenceImageUrls,
	List<String> referenceImagePaths,

	// lighting_change
	String lighting,

	// angle_change (legacy enum: front / 45 / side / top)
	String angle,

	// angle_change (v2.0 좌표)
	Double rotation,
	Double tilt,

	// ratio_change
	String ratio,

	// inpaint
	String prompt,
	String maskUrl,
	String maskPath,
	Integer maskWidth,
	Integer maskHeight,
	String size,

	// freeform / source-image / inpaint: 배경 투명 (gpt-image-1 만 지원)
	Boolean transparentBackground
) {

	public boolean hasDescription() {
		return description != null && !description.isBlank();
	}

	/** 단수/복수 어느 쪽이든 reference 이미지가 1개 이상 있는지. */
	public boolean hasReferenceImage() {
		if (referenceImageUrl != null && !referenceImageUrl.isBlank()) return true;
		if (referenceImagePath != null && !referenceImagePath.isBlank()) return true;
		if (referenceImageUrls != null && referenceImageUrls.stream().anyMatch(s -> s != null && !s.isBlank())) return true;
		if (referenceImagePaths != null && referenceImagePaths.stream().anyMatch(s -> s != null && !s.isBlank())) return true;
		return false;
	}

	/** 모든 reference URL 을 단일 리스트로 합성. path 가 있으면 우선 반환. 빈 슬롯은 제외. */
	public List<String> collectReferenceUrls() {
		java.util.ArrayList<String> out = new java.util.ArrayList<>();
		if (referenceImagePaths != null) {
			for (String p : referenceImagePaths) if (p != null && !p.isBlank()) out.add(p);
		}
		if (referenceImageUrls != null) {
			for (String u : referenceImageUrls) if (u != null && !u.isBlank()) out.add(u);
		}
		if (out.isEmpty()) {
			if (referenceImagePath != null && !referenceImagePath.isBlank()) out.add(referenceImagePath);
			else if (referenceImageUrl != null && !referenceImageUrl.isBlank()) out.add(referenceImageUrl);
		}
		return out;
	}

	public boolean hasAngleCoordinates() {
		return rotation != null || tilt != null;
	}

	public boolean isTransparentBackground() {
		return Boolean.TRUE.equals(transparentBackground);
	}

	public static EditParams empty() {
		return new EditParams(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
	}
}
