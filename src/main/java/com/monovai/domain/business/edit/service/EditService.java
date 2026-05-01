package com.monovai.domain.business.edit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.edit.dto.request.EditImageRequest;
import com.monovai.domain.business.edit.dto.response.EditCreatedResponse;
import com.monovai.domain.business.edit.repository.ImageEditRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EditService {

	private final ImageEditRepository editRepository;

	@Transactional
	public EditCreatedResponse create(Long userId, EditImageRequest request) {
		// TODO: baseId 검증 (ImageJob.id 또는 같은 root 안의 ImageEdit.id) → rootJobId 도출
		// TODO: 권한 검증 (root job 의 userId == userId)
		// TODO: ImageEdit.create 저장 → 비동기 워커 트리거 (Nanobanana)
		throw new UnsupportedOperationException("EditService.create not implemented");
	}
}