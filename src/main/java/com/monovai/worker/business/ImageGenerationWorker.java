package com.monovai.worker.business;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.monovai.worker.business.event.ImageJobCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 2 비동기 워커 — 이벤트 기반 트리거.
 * 실제 처리는 ImageGenerationProcessor 에 위임 (self-invocation 함정 회피).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationWorker {

	private final ImageGenerationProcessor processor;

	@Async("businessWorkerExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onJobCreated(ImageJobCreatedEvent event) {
		log.info("[ImageGenWorker] event received: jobId={}", event.jobId());
		try {
			processor.process(event.jobId());   // ← 외부 빈 호출 → 프록시 거침 → @Transactional 적용
		} catch (Exception e) {
			log.error("[ImageGenWorker] processJob 전체 실패: jobId={}", event.jobId(), e);
			try {
				processor.markFailed(event.jobId(), e.getMessage());
			} catch (Exception inner) {
				log.error("[ImageGenWorker] markFailed 까지 실패: jobId={}", event.jobId(), inner);
			}
		}
	}
}