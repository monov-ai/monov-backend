package com.monovai.domain.content.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.content.dto.NewsDtos.GenerateHtmlRequest;
import com.monovai.domain.content.dto.NewsDtos.GenerateHtmlResponse;
import com.monovai.domain.content.dto.NewsDtos.GenerateTextRequest;
import com.monovai.domain.content.dto.NewsDtos.GenerateTextResponse;
import com.monovai.domain.content.service.NewsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * C.2 News (관리자 전용 — /admin 프리픽스로 RBAC 자동 적용).
 */
@Tag(name = "Admin / News", description = "카드뉴스 생성 (관리자)")
@RestController
@RequestMapping("/api/v1/admin/news")
@RequiredArgsConstructor
public class NewsAdminController {

	private final NewsService newsService;

	@PostMapping("/generate-text")
	@Operation(summary = "카드뉴스 텍스트 생성")
	public ResponseEntity<GenerateTextResponse> generateText(@RequestBody GenerateTextRequest request) {
		return ResponseEntity.ok(newsService.generateText(request));
	}

	@PostMapping("/generate-html")
	@Operation(summary = "카드뉴스 HTML 생성")
	public ResponseEntity<GenerateHtmlResponse> generateHtml(@RequestBody GenerateHtmlRequest request) {
		return ResponseEntity.ok(newsService.generateHtml(request));
	}
}
