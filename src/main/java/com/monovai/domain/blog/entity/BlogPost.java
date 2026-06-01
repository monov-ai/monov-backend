package com.monovai.domain.blog.entity;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.blog.entity.enums.BlogPostStatus;
import com.monovai.domain.user.entity.User;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blog_posts",
	indexes = {
		@Index(name = "idx_blog_status_published", columnList = "status,publishedAt"),
		@Index(name = "idx_blog_locale", columnList = "locale")
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlogPost extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 160)
	private String slug;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(columnDefinition = "LONGTEXT")
	private String content;

	@Column(length = 60)
	private String category;

	@Column(length = 10)
	private String locale;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BlogPostStatus status;

	@Column(length = 200)
	private String thumbnailKey;

	@Column(length = 200)
	private String heroImageKey;

	@Column(length = 100)
	private String authorName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_user_id")
	private User authorUser;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> tags;

	private Timestamp publishedAt;

	@Builder(access = AccessLevel.PRIVATE)
	private BlogPost(String slug, String title, String summary, String content,
		String category, String locale, BlogPostStatus status,
		String thumbnailKey, String heroImageKey, String authorName, User authorUser,
		List<String> tags, Timestamp publishedAt) {
		this.slug = slug;
		this.title = title;
		this.summary = summary;
		this.content = content;
		this.category = category;
		this.locale = locale;
		this.status = status;
		this.thumbnailKey = thumbnailKey;
		this.heroImageKey = heroImageKey;
		this.authorName = authorName;
		this.authorUser = authorUser;
		this.tags = tags;
		this.publishedAt = publishedAt;
	}

	public static BlogPost create(String slug, String title, String summary, String content,
		String category, String locale, BlogPostStatus status,
		String thumbnailKey, String heroImageKey, String authorName, User authorUser,
		List<String> tags) {
		BlogPost p = BlogPost.builder()
			.slug(slug).title(title).summary(summary).content(content)
			.category(category).locale(locale).status(status)
			.thumbnailKey(thumbnailKey).heroImageKey(heroImageKey)
			.authorName(authorName).authorUser(authorUser).tags(tags)
			.build();
		if (status == BlogPostStatus.PUBLISHED) {
			p.publishedAt = new Timestamp(System.currentTimeMillis());
		}
		return p;
	}

	public void update(String title, String summary, String content,
		String category, String locale, BlogPostStatus status,
		String thumbnailKey, String heroImageKey, String authorName,
		List<String> tags) {
		if (title != null) this.title = title;
		if (summary != null) this.summary = summary;
		if (content != null) this.content = content;
		if (category != null) this.category = category;
		if (locale != null) this.locale = locale;
		if (thumbnailKey != null) this.thumbnailKey = thumbnailKey;
		if (heroImageKey != null) this.heroImageKey = heroImageKey;
		if (authorName != null) this.authorName = authorName;
		if (tags != null) this.tags = tags;
		if (status != null) {
			BlogPostStatus prev = this.status;
			this.status = status;
			if (status == BlogPostStatus.PUBLISHED && prev != BlogPostStatus.PUBLISHED && this.publishedAt == null) {
				this.publishedAt = new Timestamp(System.currentTimeMillis());
			}
		}
	}

	public void updateSlug(String slug) {
		if (slug != null && !slug.isBlank()) this.slug = slug;
	}
}
