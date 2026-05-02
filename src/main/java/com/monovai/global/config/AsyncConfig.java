package com.monovai.global.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * business 도메인 비동기 작업용 단일 스레드 풀.
	 * - corePoolSize 4: 평상시 유지
	 * - maxPoolSize 8: 피크 시 임시 확장
	 * - queueCapacity 100: 대기열
	 * - CallerRunsPolicy: 풀 + 큐 모두 꽉 차면 호출 스레드(=request thread)에서 직접 실행
	 *   → 데이터 손실 방지, HTTP 응답이 살짝 늦어지는 대신 작업은 무조건 처리
	 */
	@Bean(name = "businessWorkerExecutor")
	public TaskExecutor businessWorkerExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("biz-worker-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}