package com.monovai.worker.business;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.monovai.worker.business.event.VideoTemplateCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoTemplateWorker {

	private final VideoTemplateProcessor processor;

	@Async("businessWorkerExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCreated(VideoTemplateCreatedEvent event) {
		log.info("[VideoTemplateWorker] received id={}", event.videoTemplateId());
		try {
			processor.submit(event.videoTemplateId());
		} catch (Exception e) {
			log.error("[VideoTemplateWorker] submit 전체 실패: id={}", event.videoTemplateId(), e);
			try {
				processor.markFailed(event.videoTemplateId(), e.getMessage());
			} catch (Exception inner) {
				log.error("[VideoTemplateWorker] markFailed 까지 실패", inner);
			}
		}
	}
}
