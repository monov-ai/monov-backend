package com.monovai.domain.admin.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.admin.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * C.4 어드민. /api/v1/admin/** 는 SecurityConfig 에서 ROLE_ADMIN 으로 보호됨.
 */
@Tag(name = "Admin", description = "관리자 대시보드 / 사용자 / 크레딧 조정")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@GetMapping("/dashboard")
	@Operation(summary = "대시보드")
	public ResponseEntity<Map<String, Object>> dashboard() {
		return ResponseEntity.ok(adminService.dashboard());
	}

	@GetMapping("/feedback")
	@Operation(summary = "피드백")
	public ResponseEntity<Map<String, Object>> feedback() {
		return ResponseEntity.ok(adminService.feedback());
	}

	@GetMapping("/users")
	@Operation(summary = "사용자 검색")
	public ResponseEntity<Map<String, Object>> users(
		@RequestParam(value = "search", required = false) String search
	) {
		return ResponseEntity.ok(adminService.users(search));
	}

	@GetMapping("/users/{userId}")
	@Operation(summary = "사용자 상세")
	public ResponseEntity<Map<String, Object>> userDetail(@PathVariable("userId") Long userId) {
		return ResponseEntity.ok(adminService.userDetail(userId));
	}

	@PatchMapping("/users/{userId}")
	@Operation(summary = "사용자 크레딧 조정",
		description = "deltaLegacyCredits / deltaAdImage / deltaAiVideo / deltaImageToVideo")
	public ResponseEntity<Map<String, Object>> adjustCredits(
		@PathVariable("userId") Long userId,
		@RequestBody Map<String, Object> request
	) {
		int legacy = intOf(request, "deltaLegacyCredits");
		int adImage = intOf(request, "deltaAdImage");
		int aiVideo = intOf(request, "deltaAiVideo");
		int imageToVideo = intOf(request, "deltaImageToVideo");
		return ResponseEntity.ok(adminService.adjustCredits(userId, legacy, adImage, aiVideo, imageToVideo));
	}

	private static int intOf(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v instanceof Number n) return n.intValue();
		return 0;
	}
}
