package com.monovai.worker.business;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
import com.monovai.domain.business.imagejob.entity.enums.JobStatus;
import com.monovai.domain.business.imagejob.repository.ImageJobRepository;
import com.monovai.domain.business.prompt.service.PromptCompileService;
import com.monovai.external.nanobanana.dto.NanobananaResult;
import com.monovai.external.nanobanana.service.NanobananaService;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 별도 빈으로 분리해서 Spring AOP 프록시를 거치게 함.
 * @Transactional 이 self-invocation 함정 없이 정상 적용되려면 외부 빈에서 호출되어야 함.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationProcessor {

	private static final Duration NANOBANANA_INPUT_TTL = Duration.ofMinutes(15);

	private final ImageJobRepository jobRepository;
	private final NanobananaService nanobananaService;
	private final PromptCompileService promptCompileService;
	private final S3Service s3Service;

	@Transactional
	public void process(Long jobId) {
		ImageJob job = jobRepository.findById(jobId).orElseThrow();
		if (job.getStatus() != JobStatus.PENDING) {
			log.warn("[ImageGenProcessor] 이미 처리됨/처리 중, 무시: jobId={}, status={}", jobId, job.getStatus());
			return;
		}

		job.markRunning();

		for (ImageJobVariant v : job.getVariants()) {
			processVariant(job, v);
		}

		job.finalizeStatus();
		log.info("[ImageGenProcessor] job finalized: jobId={}, status={}", jobId, job.getStatus());
	}

	private void processVariant(ImageJob job, ImageJobVariant variant) {
		try {
			String prompt = promptCompileService.compileImageGenerationPrompt(job, variant);
			variant.markRunning(prompt);

			String sourceImageUrl = resolveSourceImageUrl(job);
			NanobananaResult result = nanobananaService.generateImage(prompt, sourceImageUrl);
			variant.markSucceeded(result.taskId(), result.s3Key());
		} catch (Exception e) {
			log.error("[ImageGenProcessor] variant 실패: jobId={}, variantSeq={}",
				job.getId(), variant.getVariantSeq(), e);
			variant.markFailed(e.getMessage());
		}
	}

	/**
	 * Nanobanana 에 첨부할 입력 이미지 URL 해석.
	 * - productImagePath(=S3 key) 우선 → 우리 S3 의 presigned URL 발급
	 * - 없으면 productImageUrl(외부 URL) 그대로 사용
	 * - 둘 다 없으면 null (textOnly 로 fallback)
	 */
	private String resolveSourceImageUrl(ImageJob job) {
		String path = job.getProductImagePath();
		if (path != null && !path.isBlank()) {
			return s3Service.getPreSignedUrlForDownload(path, NANOBANANA_INPUT_TTL);
		}
		String url = job.getProductImageUrl();
		if (url != null && !url.isBlank()) {
			return url;
		}
		return null;
	}

	@Transactional
	public void markFailed(Long jobId, String message) {
		jobRepository.findById(jobId).ifPresent(j -> j.markFailed(message));
	}
}