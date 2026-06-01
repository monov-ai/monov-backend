package com.monovai.domain.blog.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.blog.dto.request.CreateBlogPostRequest;
import com.monovai.domain.blog.dto.request.UpdateBlogPostRequest;
import com.monovai.domain.blog.dto.response.BlogPostResponse;
import com.monovai.domain.blog.entity.BlogPost;
import com.monovai.domain.blog.entity.enums.BlogPostStatus;
import com.monovai.domain.blog.repository.BlogPostRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BlogPostService {

	private static final int MAX_LIMIT = 100;
	private static final Duration IMAGE_TTL = Duration.ofDays(7);

	private final BlogPostRepository repository;
	private final UserRepository userRepository;
	private final S3Service s3Service;

	public List<BlogPostResponse> listPublic(String category, String locale, int limit) {
		Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, MAX_LIMIT)));
		List<BlogPost> posts;
		boolean hasCategory = category != null && !category.isBlank();
		boolean hasLocale = locale != null && !locale.isBlank();
		if (hasCategory && hasLocale) {
			posts = repository.findAllByStatusAndCategoryAndLocaleOrderByPublishedAtDesc(
				BlogPostStatus.PUBLISHED, category, locale, pageable);
		} else if (hasCategory) {
			posts = repository.findAllByStatusAndCategoryOrderByPublishedAtDesc(
				BlogPostStatus.PUBLISHED, category, pageable);
		} else if (hasLocale) {
			posts = repository.findAllByStatusAndLocaleOrderByPublishedAtDesc(
				BlogPostStatus.PUBLISHED, locale, pageable);
		} else {
			posts = repository.findAllByStatusOrderByPublishedAtDesc(BlogPostStatus.PUBLISHED, pageable);
		}
		return posts.stream().map(this::view).toList();
	}

	public BlogPostResponse getBySlugPublic(String slug) {
		BlogPost p = repository.findBySlug(slug)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BLOG_POST_NOT_FOUND));
		if (p.getStatus() != BlogPostStatus.PUBLISHED) {
			throw new NotFoundException(ErrorCode.BLOG_POST_NOT_FOUND);
		}
		return view(p);
	}

	public List<BlogPostResponse> listAdmin(int limit) {
		Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, MAX_LIMIT)));
		return repository.findAllByOrderByUpdatedAtDesc(pageable).stream().map(this::view).toList();
	}

	public BlogPostResponse getByIdAdmin(Long id) {
		BlogPost p = repository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BLOG_POST_NOT_FOUND));
		return view(p);
	}

	@Transactional
	public BlogPostResponse create(Long adminUserId, CreateBlogPostRequest req) {
		if (repository.existsBySlug(req.slug())) {
			throw new BusinessException(ErrorCode.DUPLICATED_BLOG_SLUG);
		}
		User author = userRepository.findById(adminUserId).orElse(null);
		BlogPostStatus status = BlogPostStatus.from(req.status());
		BlogPost p = BlogPost.create(
			req.slug(), req.title(), req.summary(), req.content(),
			req.category(), req.locale(), status,
			req.thumbnailKey(), req.heroImageKey(), req.authorName(), author,
			req.tags()
		);
		p = repository.save(p);
		log.info("[Blog] created id={} slug={} status={}", p.getId(), p.getSlug(), p.getStatus());
		return view(p);
	}

	@Transactional
	public BlogPostResponse update(Long id, UpdateBlogPostRequest req) {
		BlogPost p = repository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BLOG_POST_NOT_FOUND));

		if (req.slug() != null && !req.slug().isBlank() && !req.slug().equals(p.getSlug())) {
			if (repository.existsBySlug(req.slug())) {
				throw new BusinessException(ErrorCode.DUPLICATED_BLOG_SLUG);
			}
			p.updateSlug(req.slug());
		}

		BlogPostStatus status = req.status() == null ? null : BlogPostStatus.from(req.status());
		p.update(req.title(), req.summary(), req.content(),
			req.category(), req.locale(), status,
			req.thumbnailKey(), req.heroImageKey(), req.authorName(), req.tags());
		log.info("[Blog] updated id={} slug={} status={}", p.getId(), p.getSlug(), p.getStatus());
		return view(p);
	}

	@Transactional
	public void delete(Long id) {
		if (!repository.existsById(id)) {
			throw new NotFoundException(ErrorCode.BLOG_POST_NOT_FOUND);
		}
		repository.deleteById(id);
		log.info("[Blog] deleted id={}", id);
	}

	private BlogPostResponse view(BlogPost p) {
		String thumbUrl = p.getThumbnailKey() == null || p.getThumbnailKey().isBlank() ? null
			: s3Service.getPreSignedUrlForDownload(p.getThumbnailKey(), IMAGE_TTL);
		String heroUrl = p.getHeroImageKey() == null || p.getHeroImageKey().isBlank() ? null
			: s3Service.getPreSignedUrlForDownload(p.getHeroImageKey(), IMAGE_TTL);
		return BlogPostResponse.of(p, thumbUrl, heroUrl);
	}
}
