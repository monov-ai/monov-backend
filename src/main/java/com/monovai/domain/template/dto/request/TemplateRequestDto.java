package com.monovai.domain.template.dto.request;

import java.util.List;

public record TemplateRequestDto(
	String videoId,
	String templateId,
	String title,
	String ratio,
	String userPrompt,
	String templatePrompt,
	String model,
	String imageUrl,
	String imagePath,
	List<String> imageUrls,
	List<String> imagePaths,
	String mediaType,       // "image" | "video"
	String sourceFlow,
	String source           // "business" | "studio" | "templates"
) {
}
