package com.monovai.domain.template.controller;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.video.dto.response.VideoJobResponse;
import com.monovai.domain.business.video.entity.VideoTemplate;
import com.monovai.domain.business.video.repository.VideoTemplateRepository;
import com.monovai.domain.template.dto.request.TemplateRequestDto;
import com.monovai.domain.template.service.TemplateRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Templates / Requests", description = "템플릿 적용 요청 + 결과 폴링")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TemplateRequestController {

	private final TemplateRequestService templateRequestService;
	private final VideoTemplateRepository videoRepository;

	@PostMapping("/template-requests")
	@Operation(summary = "템플릿 적용 요청 (image/video 잡 큐잉)")
	public ResponseEntity<Map<String, Object>> create(
		@AuthenticationPrincipal Long userId,
		@RequestBody TemplateRequestDto request
	) {
		String videoId = templateRequestService.create(userId, request);
		return ResponseEntity.ok(Map.of("ok", true, "videoId", videoId));
	}

	@GetMapping("/me/template-jobs")
	@Operation(summary = "템플릿 결과 폴링 (커서 기반)",
		description = "since(ISO instant) 이후 업데이트된 잡을 updatedAt 오름차순으로 반환. 없으면 최신 limit개.")
	public ResponseEntity<Map<String, Object>> templateJobs(
		@AuthenticationPrincipal Long userId,
		@RequestParam(value = "since", required = false) String since,
		@RequestParam(value = "limit", required = false, defaultValue = "50") int limit
	) {
		int capped = Math.min(Math.max(limit, 1), 200);
		List<VideoTemplate> jobs;
		if (since != null && !since.isBlank()) {
			Timestamp ts = Timestamp.from(Instant.parse(since));
			jobs = videoRepository.findAllByUser_IdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(
				userId, ts, PageRequest.of(0, capped));
		} else {
			jobs = videoRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId, PageRequest.of(0, capped));
		}
		List<VideoJobResponse> items = jobs.stream().map(VideoJobResponse::of).toList();
		String nextCursor = jobs.isEmpty() ? since
			: jobs.get(jobs.size() - 1).getUpdatedAt().toInstant().toString();
		return ResponseEntity.ok(Map.of("items", items, "nextCursor", nextCursor == null ? "" : nextCursor));
	}
}
