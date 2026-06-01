package com.monovai.domain.blog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.blog.entity.BlogPost;
import com.monovai.domain.blog.entity.enums.BlogPostStatus;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

	Optional<BlogPost> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<BlogPost> findAllByStatusOrderByPublishedAtDesc(BlogPostStatus status, Pageable pageable);

	List<BlogPost> findAllByStatusAndLocaleOrderByPublishedAtDesc(
		BlogPostStatus status, String locale, Pageable pageable);

	List<BlogPost> findAllByStatusAndCategoryOrderByPublishedAtDesc(
		BlogPostStatus status, String category, Pageable pageable);

	List<BlogPost> findAllByStatusAndCategoryAndLocaleOrderByPublishedAtDesc(
		BlogPostStatus status, String category, String locale, Pageable pageable);

	List<BlogPost> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
