package com.monovai.domain.content.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.content.dto.WritingDtos.GenerateRequest;
import com.monovai.domain.content.dto.WritingDtos.GenerateResponse;
import com.monovai.domain.content.dto.WritingDtos.RefineRequest;
import com.monovai.domain.content.dto.WritingDtos.RefineResponse;
import com.monovai.domain.content.dto.WritingDtos.SuggestTopicsRequest;
import com.monovai.domain.content.dto.WritingDtos.SuggestTopicsResponse;
import com.monovai.domain.content.service.WritingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Content / Writing", description = "AI 글쓰기")
@RestController
@RequestMapping("/api/v1/writing")
@RequiredArgsConstructor
public class WritingController {

	private final WritingService writingService;

	@PostMapping("/suggest-topics")
	@Operation(summary = "글감 추천")
	public ResponseEntity<SuggestTopicsResponse> suggestTopics(@RequestBody SuggestTopicsRequest request) {
		return ResponseEntity.ok(writingService.suggestTopics(request));
	}

	@PostMapping("/generate")
	@Operation(summary = "글 생성")
	public ResponseEntity<GenerateResponse> generate(@RequestBody GenerateRequest request) {
		return ResponseEntity.ok(writingService.generate(request));
	}

	@PostMapping("/refine")
	@Operation(summary = "글 다듬기")
	public ResponseEntity<RefineResponse> refine(@RequestBody RefineRequest request) {
		return ResponseEntity.ok(writingService.refine(request));
	}
}
