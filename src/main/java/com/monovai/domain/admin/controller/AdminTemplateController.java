package com.monovai.domain.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monovai.domain.admin.service.AdminTemplateService;
import com.monovai.domain.template.dto.response.TemplateView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin / Templates", description = "템플릿 관리 (관리자)")
@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
public class AdminTemplateController {

	private final AdminTemplateService service;

	@GetMapping
	@Operation(summary = "템플릿 목록 (관리자)")
	public ResponseEntity<List<TemplateView>> list() {
		return ResponseEntity.ok(service.list().stream().map(service::toView).toList());
	}

	@PostMapping
	@Operation(summary = "템플릿 생성")
	public ResponseEntity<TemplateView> create(@RequestBody Map<String, Object> body) {
		return ResponseEntity.ok(service.toView(service.create(body)));
	}

	@GetMapping("/stats")
	@Operation(summary = "템플릿 통계")
	public ResponseEntity<Map<String, Object>> stats() {
		return ResponseEntity.ok(service.stats());
	}

	@PatchMapping("/{id}")
	@Operation(summary = "템플릿 수정")
	public ResponseEntity<TemplateView> update(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
		return ResponseEntity.ok(service.toView(service.update(id, body)));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "템플릿 삭제")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") String id) {
		service.delete(id);
		return ResponseEntity.ok(Map.of("ok", true));
	}

	@PostMapping(value = "/upload", consumes = {"multipart/form-data"})
	@Operation(summary = "템플릿 파일 업로드")
	public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
		return ResponseEntity.ok(service.upload(file));
	}
}
