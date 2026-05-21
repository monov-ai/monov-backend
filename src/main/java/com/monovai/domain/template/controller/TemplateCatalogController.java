package com.monovai.domain.template.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.template.dto.response.TemplateView;
import com.monovai.domain.template.service.TemplateCatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Templates", description = "템플릿 카탈로그 / 즐겨찾기")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TemplateCatalogController {

	private final TemplateCatalogService service;

	@GetMapping("/templates")
	@Operation(summary = "템플릿 목록")
	public ResponseEntity<List<TemplateView>> list() {
		return ResponseEntity.ok(service.list());
	}

	@GetMapping("/templates/{id}")
	@Operation(summary = "템플릿 단건")
	public ResponseEntity<TemplateView> get(@PathVariable("id") String id) {
		return ResponseEntity.ok(service.get(id));
	}

	@PostMapping("/templates/{id}/downloads")
	@Operation(summary = "다운로드 카운트 증가")
	public ResponseEntity<Map<String, Object>> download(@PathVariable("id") String id) {
		long count = service.incrementDownload(id);
		return ResponseEntity.ok(Map.of("ok", true, "downloadCount", count));
	}

	@GetMapping("/me/favorite-templates")
	@Operation(summary = "즐겨찾기 템플릿 목록")
	public ResponseEntity<Map<String, Object>> listFavorites(@AuthenticationPrincipal Long userId) {
		TemplateCatalogService.FavoriteList result = service.listFavorites(userId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("items", result.items());
		body.put("ids", result.ids());
		return ResponseEntity.ok(body);
	}

	@PostMapping("/me/favorite-templates")
	@Operation(summary = "즐겨찾기 토글")
	public ResponseEntity<Map<String, Object>> toggleFavorite(
		@AuthenticationPrincipal Long userId,
		@RequestBody Map<String, Object> request
	) {
		String templateId = String.valueOf(request.get("templateId"));
		boolean favorite = Boolean.TRUE.equals(request.get("favorite"));
		service.toggleFavorite(userId, templateId, favorite);
		return ResponseEntity.ok(Map.of("ok", true, "templateId", templateId, "favorite", favorite));
	}
}
