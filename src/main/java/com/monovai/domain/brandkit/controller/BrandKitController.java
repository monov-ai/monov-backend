package com.monovai.domain.brandkit.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.brandkit.entity.BrandKitAccount;
import com.monovai.domain.brandkit.service.BrandKitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * C.3 Studio 브랜드 키트. 평문 {@code { ok, ... }} 응답 (SDK 가 그대로 사용).
 */
@Tag(name = "Studio / Brand Kit", description = "마케팅 자동화 브랜드 키트")
@RestController
@RequestMapping("/api/v1/me/brand-kit")
@RequiredArgsConstructor
public class BrandKitController {

	private final BrandKitService service;

	@GetMapping
	@Operation(summary = "브랜드 키트 조회")
	public ResponseEntity<Map<String, Object>> get(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(ok("brandKit", service.getBrandKit(userId)));
	}

	@PostMapping
	@Operation(summary = "브랜드 키트 upsert")
	public ResponseEntity<Map<String, Object>> upsert(
		@AuthenticationPrincipal Long userId,
		@RequestBody Object info
	) {
		return ResponseEntity.ok(ok("brandKit", service.upsertBrandKit(userId, info)));
	}

	@GetMapping("/accounts")
	@Operation(summary = "연결 계정 목록")
	public ResponseEntity<Map<String, Object>> listAccounts(@AuthenticationPrincipal Long userId) {
		List<BrandKitAccount> accounts = service.listAccounts(userId);
		List<Map<String, Object>> items = accounts.stream().map(a -> {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("accountId", a.getAccountId());
			m.put("data", a.getData());
			return m;
		}).toList();
		return ResponseEntity.ok(ok("accounts", items));
	}

	@PostMapping("/accounts")
	@Operation(summary = "연결 계정 추가")
	public ResponseEntity<Map<String, Object>> createAccount(
		@AuthenticationPrincipal Long userId,
		@RequestBody Object data
	) {
		BrandKitAccount acc = service.createAccount(userId, data);
		Map<String, Object> body = ok("accountId", acc.getAccountId());
		body.put("data", acc.getData());
		return ResponseEntity.ok(body);
	}

	@PatchMapping("/accounts/{accountId}")
	@Operation(summary = "연결 계정 수정")
	public ResponseEntity<Map<String, Object>> updateAccount(
		@AuthenticationPrincipal Long userId,
		@PathVariable("accountId") String accountId,
		@RequestBody Object data
	) {
		BrandKitAccount acc = service.updateAccount(userId, accountId, data);
		Map<String, Object> body = ok("accountId", acc.getAccountId());
		body.put("data", acc.getData());
		return ResponseEntity.ok(body);
	}

	@DeleteMapping("/accounts/{accountId}")
	@Operation(summary = "연결 계정 삭제")
	public ResponseEntity<Map<String, Object>> deleteAccount(
		@AuthenticationPrincipal Long userId,
		@PathVariable("accountId") String accountId
	) {
		service.deleteAccount(userId, accountId);
		return ResponseEntity.ok(Map.of("ok", true));
	}

	@GetMapping("/plans/{weekId}")
	@Operation(summary = "주차 계획 조회")
	public ResponseEntity<Map<String, Object>> getPlan(
		@AuthenticationPrincipal Long userId,
		@PathVariable("weekId") String weekId
	) {
		return ResponseEntity.ok(ok("plan", service.getPlan(userId, weekId)));
	}

	@PutMapping("/plans/{weekId}")
	@Operation(summary = "주차 계획 저장 (전체)")
	public ResponseEntity<Map<String, Object>> putPlan(
		@AuthenticationPrincipal Long userId,
		@PathVariable("weekId") String weekId,
		@RequestBody Object data
	) {
		return ResponseEntity.ok(ok("plan", service.upsertPlan(userId, weekId, data)));
	}

	@PatchMapping("/plans/{weekId}")
	@Operation(summary = "주차 계획 부분 수정 (§6 deep merge)",
		description = "객체끼리만 재귀 병합. 배열/스칼라는 incoming 값으로 교체.")
	public ResponseEntity<Map<String, Object>> patchPlan(
		@AuthenticationPrincipal Long userId,
		@PathVariable("weekId") String weekId,
		@RequestBody Object data
	) {
		return ResponseEntity.ok(ok("plan", service.patchPlan(userId, weekId, data)));
	}

	private static Map<String, Object> ok(String key, Object value) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("ok", true);
		m.put(key, value);
		return m;
	}
}
