package com.monovai.domain.payment.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.credit.dto.response.CreditBalanceResponse;
import com.monovai.domain.credit.entity.enums.CreditTxnType;
import com.monovai.domain.credit.service.CreditService;
import com.monovai.domain.payment.PaymentCatalog;
import com.monovai.domain.payment.dto.request.ConfirmPaymentRequest;
import com.monovai.domain.payment.entity.Payment;
import com.monovai.domain.payment.entity.enums.PaymentType;
import com.monovai.domain.payment.repository.PaymentRepository;
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
 * B.1 단건 결제 confirm. Toss 승인 → 패키지 매핑 크레딧 충전 → Payment 기록.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

	private final TossClient tossClient;
	private final PaymentRepository paymentRepository;
	private final UserRepository userRepository;
	private final CreditService creditService;

	@Transactional
	public Map<String, Object> confirm(Long userId, ConfirmPaymentRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		PaymentCatalog.Package pkg = PaymentCatalog.findPackage(request.packageId())
			.orElseThrow(() -> new BadRequestException(ErrorCode.UNKNOWN_PACKAGE));

		if (request.amount() == null || request.amount() != pkg.amount()) {
			throw new BadRequestException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}

		// 멱등: 이미 처리된 paymentKey 면 재충전 금지
		if (paymentRepository.existsByPaymentKey(request.paymentKey())) {
			throw new BadRequestException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}

		Map<String, Object> tossResult = tossClient.confirmPayment(
			request.paymentKey(), request.orderId(), request.amount());

		String status = String.valueOf(tossResult.get("status"));
		if (!"DONE".equalsIgnoreCase(status)) {
			paymentRepository.save(Payment.create(user, request.paymentKey(), request.orderId(),
				request.amount(), pkg.id(), null, PaymentType.ONE_TIME, status, "not_done", tossResult));
			throw new BusinessException(ErrorCode.TOSS_CONFIRM_FAILED);
		}

		int balance = creditService.grant(userId, CreditTxnType.GRANT_ONE_TIME, pkg.credits(),
			"단건 결제 충전: " + pkg.id(), "payment:" + request.paymentKey());

		paymentRepository.save(Payment.create(user, request.paymentKey(), request.orderId(),
			request.amount(), pkg.id(), null, PaymentType.ONE_TIME, "DONE", null, tossResult));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", true);
		body.put("grantedCredits", pkg.credits());
		body.put("wallet", CreditBalanceResponse.of(creditService.getWallet(userId)).usage());
		body.put("balance", balance);
		body.put("paymentKey", request.paymentKey());
		return body;
	}
}
