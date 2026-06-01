package com.monovai.domain.blog.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.blog.dto.response.BlogPostResponse;
import com.monovai.domain.blog.service.BlogPostService;
import com.monovai.global.annotation.DisableSwaggerSecurity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * §17: 공개 블로그 API. 모든 사용자 접근 가능 (인증 불필요).
 */
@Tag(name = "Blog", description = "공개 블로그 글")
@RestController
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
public class BlogController {

	private final BlogPostService service;

	@GetMapping("/posts")
	@DisableSwaggerSecurity
	@Operation(summary = "발행된 글 목록", description = "category / locale 필터 가능. 기본 limit=20, 최대 100.")
	public ResponseEntity<Map<String, Object>> list(
		@RequestParam(value = "category", required = false) String category,
		@RequestParam(value = "locale", required = false) String locale,
		@RequestParam(value = "limit", required = false, defaultValue = "20") int limit
	) {
		List<BlogPostResponse> items = service.listPublic(category, locale, limit);
		return ResponseEntity.ok(Map.of("ok", true, "items", items));
	}

	@GetMapping("/posts/{slug}")
	@DisableSwaggerSecurity
	@Operation(summary = "발행된 글 단건 (slug)")
	public ResponseEntity<Map<String, Object>> getBySlug(@PathVariable("slug") String slug) {
		return ResponseEntity.ok(Map.of("ok", true, "post", service.getBySlugPublic(slug)));
	}
}
