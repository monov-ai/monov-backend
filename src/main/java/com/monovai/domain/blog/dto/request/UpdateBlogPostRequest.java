package com.monovai.domain.blog.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;

public record UpdateBlogPostRequest(
	@Size(max = 160)
	String slug,

	@Size(max = 200)
	String title,

	String summary,

	String content,

	@Size(max = 60)
	String category,

	@Size(max = 10)
	String locale,

	String status,

	@Size(max = 200)
	String thumbnailKey,

	@Size(max = 200)
	String heroImageKey,

	@Size(max = 100)
	String authorName,

	List<String> tags
) {
}
