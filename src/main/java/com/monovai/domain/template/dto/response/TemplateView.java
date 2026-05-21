package com.monovai.domain.template.dto.response;

import java.util.List;

import com.monovai.domain.template.entity.Template;

public record TemplateView(
	String id,
	String title,
	String shortDescription,
	String category,
	int credit,
	String mediaType,
	String model,
	String ratio,
	List<String> tags,
	String thumbnailUrl,
	String fileUrl,
	long downloadCount,
	long usageCount
) {
	public static TemplateView of(Template t, String signedUrl) {
		return new TemplateView(
			t.getId(),
			t.getTitle(),
			t.getShortDescription(),
			t.getCategory(),
			t.getCredit(),
			t.getMediaType(),
			t.getModel(),
			null,
			t.hashtagValues(),
			signedUrl != null ? signedUrl : t.getImageUrl(),
			signedUrl != null ? signedUrl : t.getImageUrl(),
			t.getDownloadCount(),
			t.getUsageCount()
		);
	}
}
