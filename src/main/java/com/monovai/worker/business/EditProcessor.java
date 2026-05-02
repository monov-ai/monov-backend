package com.monovai.worker.business;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.enums.EditStatus;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.edit.repository.ImageEditRepository;
import com.monovai.domain.business.prompt.service.PromptCompileService;
import com.monovai.external.nanobanana.dto.NanobananaResult;
import com.monovai.external.nanobanana.service.NanobananaService;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 3 edit 처리.
 * - base 이미지(첫 번째 첨부) + 선택적으로 reference 이미지(두 번째 첨부) 를 Nanobanana 에 보냄.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EditProcessor {

	private static final Duration NANOBANANA_INPUT_TTL = Duration.ofMinutes(15);

	private final ImageEditRepository editRepository;
	private final NanobananaService nanobananaService;
	private final PromptCompileService promptCompileService;
	private final S3Service s3Service;

	@Transactional
	public void process(Long editId) {
		ImageEdit edit = editRepository.findById(editId).orElseThrow();
		if (edit.getStatus() != EditStatus.PENDING) {
			log.warn("[EditProcessor] 이미 처리됨/처리 중, 무시: editId={}, status={}", editId, edit.getStatus());
			return;
		}

		try {
			String prompt = promptCompileService.compileEditPrompt(edit);
			edit.markRunning(prompt);

			String baseUrl = baseImageUrl(edit);
			String referenceUrl = referenceImageUrl(edit);

			NanobananaResult result;
			if (referenceUrl != null) {
				result = nanobananaService.generateImage(prompt, baseUrl, referenceUrl);
			} else {
				result = nanobananaService.generateImage(prompt, baseUrl);
			}
			edit.markSucceeded(result.taskId(), result.s3Key());
		} catch (Exception e) {
			log.error("[EditProcessor] 처리 실패: editId={}", editId, e);
			edit.markFailed(e.getMessage());
		}
	}

	private String baseImageUrl(ImageEdit edit) {
		String key = edit.getBaseS3Key();
		if (key == null || key.isBlank()) {
			throw new IllegalStateException("baseS3Key 가 비어있습니다");
		}
		return s3Service.getPreSignedUrlForDownload(key, NANOBANANA_INPUT_TTL);
	}

	private String referenceImageUrl(ImageEdit edit) {
		EditParams p = edit.getParams();
		if (p == null) return null;
		if (p.referenceImagePath() != null && !p.referenceImagePath().isBlank()) {
			return s3Service.getPreSignedUrlForDownload(p.referenceImagePath(), NANOBANANA_INPUT_TTL);
		}
		if (p.referenceImageUrl() != null && !p.referenceImageUrl().isBlank()) {
			return p.referenceImageUrl();
		}
		return null;
	}

	@Transactional
	public void markFailed(Long editId, String message) {
		editRepository.findById(editId).ifPresent(e -> e.markFailed(message));
	}
}