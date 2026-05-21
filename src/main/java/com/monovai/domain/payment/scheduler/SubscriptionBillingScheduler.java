package com.monovai.domain.payment.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.payment.entity.Subscription;
import com.monovai.domain.payment.entity.enums.SubscriptionStatus;
import com.monovai.domain.payment.repository.SubscriptionRepository;
import com.monovai.domain.payment.service.BillingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * B.5 정기결제 cron. 매일 04:00 (KST) 에 nextBillingDate <= today 이고 active 인 구독 결제 시도.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBillingScheduler {

	private final SubscriptionRepository subscriptionRepository;
	private final BillingService billingService;

	@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
	@Transactional
	public void runDailyBilling() {
		LocalDate today = LocalDate.now();
		List<Subscription> due = subscriptionRepository
			.findAllByStatusAndNextBillingDateLessThanEqual(SubscriptionStatus.ACTIVE, today);
		log.info("[BillingCron] 대상 구독 {}건", due.size());
		int ok = 0;
		int fail = 0;
		for (Subscription s : due) {
			boolean success = billingService.chargeRenewal(s);
			if (success) ok++; else fail++;
		}
		log.info("[BillingCron] 완료 성공={} 실패={}", ok, fail);
	}
}
