package com.monovai.domain.blog.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBlogPostRequest(
	@NotBlank(message = "slug 는 필수입니다.")
	@Size(max = 160)
	String slug,

	@NotBlank(message = "title 은 필수입니다.")
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
