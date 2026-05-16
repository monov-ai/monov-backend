package com.monovai.worker.business;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.video.entity.VideoTemplate;
import com.monovai.domain.business.video.entity.enums.VideoStatus;
import com.monovai.domain.business.video.repository.VideoTemplateRepository;
import com.monovai.external.kie.service.KieService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoTemplateProcessor {

	private final VideoTemplateRepository repository;
	private final KieService kieService;

	@Transactional
	public void submit(Long videoTemplateId) {
		VideoTemplate v = repository.findById(videoTemplateId).orElseThrow();
		if (v.getStatus() != VideoStatus.REQUESTED) {
			log.warn("[VideoTemplateProcessor] 이미 처리됨: id={} status={}", videoTemplateId, v.getStatus());
			return;
		}
		try {
			String jobId = kieService.submitImageToVideo(
				v.getSourceImageUrl(), v.getUserPrompt(), v.getTemplatePrompt(), v.getRatio()
			);
			v.markRunning(jobId);
		} catch (Exception e) {
			log.error("[VideoTemplateProcessor] submit 실패 id={}", videoTemplateId, e);
			v.markFailed(e.getMessage());
		}
	}

	@Transactional
	public void poll(Long videoTemplateId) {
		VideoTemplate v = repository.findById(videoTemplateId).orElseThrow();
		if (v.getStatus() != VideoStatus.RUNNING || v.getKieTaskId() == null) return;
		Map<String, Object> res = kieService.pollJob(v.getKieTaskId());
		Object status = res.get("status");
		Object url = res.get("resultUrl");
		if ("completed".equalsIgnoreCase(String.valueOf(status)) && url != null) {
			v.markCompleted(String.valueOf(url), null);
		} else if ("failed".equalsIgnoreCase(String.valueOf(status))) {
			v.markFailed(String.valueOf(res.getOrDefault("error", "unknown")));
		}
	}

	@Transactional
	public void markFailed(Long videoTemplateId, String message) {
		repository.findById(videoTemplateId).ifPresent(v -> v.markFailed(message));
	}
}
