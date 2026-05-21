package com.monovai.domain.content.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.content.entity.NewsTemplate;
import com.monovai.domain.content.repository.NewsTemplateRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.NotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin / News Templates", description = "카드뉴스 템플릿 관리 (관리자)")
@RestController
@RequestMapping("/api/v1/admin/news/templates")
@RequiredArgsConstructor
public class NewsTemplateAdminController {

	private final NewsTemplateRepository repository;

	@GetMapping
	@Operation(summary = "뉴스 템플릿 목록")
	public ResponseEntity<List<NewsTemplate>> list() {
		return ResponseEntity.ok(repository.findAll());
	}

	@PostMapping
	@Operation(summary = "뉴스 템플릿 생성")
	@Transactional
	public ResponseEntity<NewsTemplate> create(@RequestBody Map<String, Object> body) {
		String id = body.get("id") != null ? body.get("id").toString()
			: "news_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		String name = body.get("name") != null ? body.get("name").toString() : "Untitled";
		return ResponseEntity.ok(repository.save(NewsTemplate.create(id, name, body.get("data"))));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "뉴스 템플릿 수정")
	@Transactional
	public ResponseEntity<NewsTemplate> update(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
		NewsTemplate t = repository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
		t.update(body.get("name") != null ? body.get("name").toString() : null, body.get("data"));
		return ResponseEntity.ok(t);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "뉴스 템플릿 삭제")
	@Transactional
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") String id) {
		NewsTemplate t = repository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
		repository.delete(t);
		return ResponseEntity.ok(Map.of("ok", true));
	}
}
