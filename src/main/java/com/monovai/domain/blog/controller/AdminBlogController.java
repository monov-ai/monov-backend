package com.monovai.domain.blog.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monovai.domain.blog.dto.request.CreateBlogPostRequest;
import com.monovai.domain.blog.dto.request.UpdateBlogPostRequest;
import com.monovai.domain.blog.dto.response.BlogPostResponse;
import com.monovai.domain.blog.service.BlogPostService;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.BusinessException;
import com.monovai.infrastructure.s3.service.S3Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * §17: 어드민 블로그 CRUD + 이미지 업로드. /api/v1/admin/** 는 SecurityConfig 에서 ADMIN role 강제.
 */
@Tag(name = "Admin / Blog", description = "어드민 블로그 CRUD")
@RestController
@RequestMapping("/api/v1/admin/blog")
@RequiredArgsConstructor
public class AdminBlogController {

	private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;

	private final BlogPostService service;
	private final S3Service s3Service;

	@GetMapping("/posts")
	@Operation(summary = "전체 글 목록 (draft 포함)")
	public ResponseEntity<Map<String, Object>> list(
		@RequestParam(value = "limit", required = false, defaultValue = "50") int limit
	) {
		List<BlogPostResponse> items = service.listAdmin(limit);
		return ResponseEntity.ok(Map.of("ok", true, "items", items));
	}

	@GetMapping("/posts/{id}")
	@Operation(summary = "글 단건 (admin view)")
	public ResponseEntity<Map<String, Object>> get(@PathVariable("id") Long id) {
		return ResponseEntity.ok(Map.of("ok", true, "post", service.getByIdAdmin(id)));
	}

	@PostMapping("/posts")
	@Operation(summary = "글 생성")
	public ResponseEntity<Map<String, Object>> create(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CreateBlogPostRequest request
	) {
		return ResponseEntity.ok(Map.of("ok", true, "post", service.create(userId, request)));
	}

	@PatchMapping("/posts/{id}")
	@Operation(summary = "글 수정")
	public ResponseEntity<Map<String, Object>> update(
		@PathVariable("id") Long id,
		@Valid @RequestBody UpdateBlogPostRequest request
	) {
		return ResponseEntity.ok(Map.of("ok", true, "post", service.update(id, request)));
	}

	@DeleteMapping("/posts/{id}")
	@Operation(summary = "글 삭제")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Long id) {
		service.delete(id);
		return ResponseEntity.ok(Map.of("ok", true));
	}

	@PostMapping("/upload")
	@Operation(summary = "블로그용 이미지 업로드 (multipart)",
		description = "최대 10MB. S3 key 와 7d presigned URL 반환.")
	public ResponseEntity<Map<String, Object>> upload(
		@AuthenticationPrincipal Long userId,
		@RequestParam("file") MultipartFile file
	) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException(ErrorCode.MISSING_FILE);
		}
		if (file.getSize() > MAX_UPLOAD_BYTES) {
			throw new BadRequestException(ErrorCode.FILE_TOO_LARGE);
		}
		String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
		String ext = guessExtension(contentType, file.getOriginalFilename());
		String key = "blog/" + userId + "/" + System.currentTimeMillis()
			+ "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
		try {
			s3Service.uploadBytes(key, file.getBytes(), contentType);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
		}
		String url = s3Service.getPreSignedUrlForDownload(key, java.time.Duration.ofDays(7));
		return ResponseEntity.ok(Map.of("ok", true, "key", key, "url", url));
	}

	private static String guessExtension(String contentType, String filename) {
		if (filename != null) {
			int dot = filename.lastIndexOf('.');
			if (dot >= 0 && dot < filename.length() - 1) {
				String e = filename.substring(dot).toLowerCase();
				if (e.matches("\\.(png|jpg|jpeg|webp|gif|svg)")) return e;
			}
		}
		return switch (contentType) {
			case "image/png" -> ".png";
			case "image/jpeg" -> ".jpg";
			case "image/webp" -> ".webp";
			case "image/gif" -> ".gif";
			case "image/svg+xml" -> ".svg";
			default -> ".bin";
		};
	}
}
