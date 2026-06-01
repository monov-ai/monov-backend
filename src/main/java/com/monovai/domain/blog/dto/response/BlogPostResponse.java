package com.monovai.domain.blog.dto.response;

import java.time.Instant;
import java.util.List;

import com.monovai.domain.blog.entity.BlogPost;

public record BlogPostResponse(
	Long id,
	String slug,
	String title,
	String summary,
	String content,
	String category,
	String locale,
	String status,
	String thumbnailKey,
	String thumbnailUrl,
	String heroImageKey,
	String heroImageUrl,
	String authorName,
	List<String> tags,
	Instant publishedAt,
	Instant createdAt,
	Instant updatedAt
) {
	public static BlogPostResponse of(BlogPost p, String thumbnailUrl, String heroImageUrl) {
		return new BlogPostResponse(
			p.getId(),
			p.getSlug(),
			p.getTitle(),
			p.getSummary(),
			p.getContent(),
			p.getCategory(),
			p.getLocale(),
			p.getStatus().getValue(),
			p.getThumbnailKey(),
			thumbnailUrl,
			p.getHeroImageKey(),
			heroImageUrl,
			p.getAuthorName(),
			p.getTags(),
			p.getPublishedAt() != null ? p.getPublishedAt().toInstant() : null,
			p.getCreatedAt() != null ? p.getCreatedAt().toInstant() : null,
			p.getUpdatedAt() != null ? p.getUpdatedAt().toInstant() : null
		);
	}
}
