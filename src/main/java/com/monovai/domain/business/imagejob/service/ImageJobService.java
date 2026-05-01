package com.monovai.domain.business.imagejob.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.imagejob.dto.request.GenerateImageRequest;
import com.monovai.domain.business.imagejob.dto.response.ImageJobCreatedResponse;
import com.monovai.domain.business.imagejob.dto.response.ImageJobResponse;
import com.monovai.domain.business.imagejob.repository.ImageJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ImageJobService {

	private final ImageJobRepository imageJobRepository;

	@Transactional
	public ImageJobCreatedResponse create(Long userId, GenerateImageRequest request) {
		// TODO: ImageJob (PENDING) 저장 → 비동기 워커 트리거 → jobId 반환
		// 1. 권한 검증 (recommendation 소유자 == userId)
		// 2. ImageJob.create 저장
		// 3. ImageGenerationWorker 비동기 호출 (Nanobanana)
		throw new UnsupportedOperationException("ImageJobService.create not implemented");
	}

	public ImageJobResponse get(Long userId, Long jobId) {
		// TODO: 권한 검증 + job 조회 + 같은 root 의 edits[] 함께 조회 (v1.1)
		throw new UnsupportedOperationException("ImageJobService.get not implemented");
	}
}