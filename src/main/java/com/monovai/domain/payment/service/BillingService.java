package com.monovai.domain.payment.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.credit.dto.response.CreditBalanceResponse;
import com.monovai.domain.credit.entity.UsageWallet;
import com.monovai.domain.credit.entity.enums.CreditTxnType;
import com.monovai.domain.credit.service.CreditService;
import com.monovai.domain.payment.PaymentCatalog;
import com.monovai.domain.payment.dto.request.IssueBillingRequest;
import com.monovai.domain.payment.dto.request.PayBillingRequest;
import com.monovai.domain.payment.entity.Billing;
import com.monovai.domain.payment.entity.Payment;
import com.monovai.domain.payment.entity.Subscription;
import com.monovai.domain.payment.entity.enums.PaymentType;
import com.monovai.domain.payment.entity.enums.SubscriptionStatus;
import com.monovai.domain.payment.repository.BillingRepository;
import com.monovai.domain.payment.repository.PaymentRepository;
import com.monovai.domain.payment.repository.SubscriptionRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.external.toss.service.TossClient;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.BusinessException;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * B.2 빌링키 발급 / B.3 즉시 결제 / B.4 해지. cron(B.5) 도 chargeSubscription 재사용.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BillingService {

	private final TossClient tossClient;
	private final BillingRepository billingRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final PaymentRepository paymentRepository;
	private final UserRepository userRepository;
	private final CreditService creditService;

	@Transactional
	public Map<String, Object> issue(Long userId, IssueBillingRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
		PaymentCatalog.findPlan(request.planId())
			.orElseThrow(() -> new BadRequestException(ErrorCode.UNKNOWN_PACKAGE));

		String customerKey = "cust_" + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		Map<String, Object> result = tossClient.issueBillingKey(request.authKey(), customerKey);
		String billingKey = String.valueOf(result.get("billingKey"));
		if (billingKey == null || "null".equals(billingKey)) {
			throw new BusinessException(ErrorCode.TOSS_BILLING_FAILED);
		}
		String cardLast4 = extractCard(result, "number");
		String cardBrand = extractCard(result, "cardType");

		Billing billing = Billing.create(user, billingKey, customerKey, cardLast4, cardBrand, request.planId());
		billingRepository.save(billing);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", true);
		body.put("billingKey", billingKey);
		body.put("customerKey", customerKey);
		return body;
	}

	@Transactional
	public Map<String, Object> pay(Long userId, PayBillingRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		subscriptionRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
			.ifPresent(s -> {
				throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
			});

		PaymentCatalog.Plan plan = PaymentCatalog.findPlan(request.planId())
			.orElseThrow(() -> new BadRequestException(ErrorCode.UNKNOWN_PACKAGE));

		Billing billing = billingRepository.findByBillingKey(request.billingKey())
			.orElseThrow(() -> new NotFoundException(ErrorCode.BILLING_KEY_NOT_FOUND));

		String orderId = "mvk_sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		Map<String, Object> result = tossClient.payWithBillingKey(
			billing.getBillingKey(), billing.getCustomerKey(), plan.amount(),
			orderId, plan.name(), user.getEmail());

		String status = String.valueOf(result.get("status"));
		if (!"DONE".equalsIgnoreCase(status)) {
			paymentRepository.save(Payment.create(user, str(result.get("paymentKey")), orderId, plan.amount(),
				null, plan.id(), PaymentType.SUBSCRIPTION, status, "not_done", result));
			throw new BusinessException(ErrorCode.TOSS_BILLING_FAILED);
		}

		Subscription subscription = Subscription.create(user, plan.id(), plan.name(),
			billing.getBillingKey(), billing.getCustomerKey(),
			plan.legacyCredits(), plan.adImage(), plan.aiVideo(), plan.imageToVideo());
		subscriptionRepository.save(subscription);

		UsageWallet wallet = creditService.getOrCreateWallet(userId);
		wallet.resetMonthlyAllowance(plan.legacyCredits(), plan.adImage(), plan.aiVideo(), plan.imageToVideo());
		creditService.grant(userId, CreditTxnType.GRANT_SUBSCRIPTION, 0,
			"구독 시작 충전: " + plan.id(), "sub_init:" + subscription.getId());

		paymentRepository.save(Payment.create(user, str(result.get("paymentKey")), orderId, plan.amount(),
			null, plan.id(), PaymentType.SUBSCRIPTION, "DONE", null, result));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", true);
		body.put("grantedCredits", plan.legacyCredits());
		body.put("monthlyAllowance", Map.of("legacyCredits", plan.legacyCredits()));
		body.put("wallet", CreditBalanceResponse.of(wallet).usage());
		body.put("paymentKey", result.get("paymentKey"));
		body.put("nextBillingDate", subscription.getNextBillingDate().format(DateTimeFormatter.ISO_DATE));
		return body;
	}

	@Transactional
	public Map<String, Object> cancel(Long userId) {
		Subscription subscription = subscriptionRepository
			.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
			.orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
		subscription.cancel();
		billingRepository.findFirstByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId)
			.ifPresent(Billing::deactivate);

		String periodEnd = subscription.getCurrentPeriodEnd().toLocalDate().format(DateTimeFormatter.ISO_DATE);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", true);
		body.put("message", "구독이 취소되었습니다. " + periodEnd + " 까지 이용 가능합니다.");
		body.put("currentPeriodEnd", periodEnd);
		return body;
	}

	/**
	 * B.5 cron 재사용: 활성 구독 1건 갱신 결제 시도. 성공 true / 실패 false.
	 */
	@Transactional
	public boolean chargeRenewal(Subscription subscription) {
		PaymentCatalog.Plan plan = PaymentCatalog.findPlan(subscription.getPlanId()).orElse(null);
		if (plan == null) {
			subscription.cancel();
			return false;
		}
		User user = subscription.getUser();
		String orderId = "mvk_renew_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		try {
			Map<String, Object> result = tossClient.payWithBillingKey(
				subscription.getBillingKey(), subscription.getCustomerKey(), plan.amount(),
				orderId, plan.name(), user.getEmail());
			String status = String.valueOf(result.get("status"));
			if (!"DONE".equalsIgnoreCase(status)) {
				subscription.markPastDue();
				if (subscription.getRetryCount() >= 3) subscription.cancel();
				paymentRepository.save(Payment.create(user, str(result.get("paymentKey")), orderId, plan.amount(),
					null, plan.id(), PaymentType.SUBSCRIPTION, status, "renew_failed", result));
				return false;
			}
			subscription.renew();
			UsageWallet wallet = creditService.getOrCreateWallet(user.getId());
			wallet.resetMonthlyAllowance(plan.legacyCredits(), plan.adImage(), plan.aiVideo(), plan.imageToVideo());
			creditService.grant(user.getId(), CreditTxnType.GRANT_SUBSCRIPTION, 0,
				"구독 갱신 충전: " + plan.id(), "sub_renew:" + subscription.getId() + ":" + orderId);
			paymentRepository.save(Payment.create(user, str(result.get("paymentKey")), orderId, plan.amount(),
				null, plan.id(), PaymentType.SUBSCRIPTION, "DONE", null, result));
			return true;
		} catch (Exception e) {
			log.error("[Billing] renewal 실패 subId={}", subscription.getId(), e);
			subscription.markPastDue();
			if (subscription.getRetryCount() >= 3) subscription.cancel();
			return false;
		}
	}

	private static String str(Object o) {
		return o == null ? null : o.toString();
	}

	@SuppressWarnings("unchecked")
	private static String extractCard(Map<String, Object> result, String field) {
		Object card = result.get("card");
		if (card instanceof Map<?, ?> m) {
			Object v = ((Map<String, Object>) m).get(field);
			return v == null ? null : v.toString();
		}
		return null;
	}
}
