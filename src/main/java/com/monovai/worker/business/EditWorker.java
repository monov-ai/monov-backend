package com.monovai.worker.business;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.monovai.worker.business.event.EditCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EditWorker {

	private final EditProcessor processor;

	@Async("businessWorkerExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onEditCreated(EditCreatedEvent event) {
		log.info("[EditWorker] event received: editId={}", event.editId());
		try {
			processor.process(event.editId());
		} catch (Exception e) {
			log.error("[EditWorker] processEdit 전체 실패: editId={}", event.editId(), e);
			try {
				processor.markFailed(event.editId(), e.getMessage());
			} catch (Exception inner) {
				log.error("[EditWorker] markFailed 까지 실패: editId={}", event.editId(), inner);
			}
		}
	}
}