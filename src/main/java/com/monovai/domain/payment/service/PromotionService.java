package com.monovai.domain.payment.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.credit.entity.enums.CreditTxnType;
import com.monovai.domain.credit.service.CreditService;
import com.monovai.domain.payment.dto.request.CreatePromotionRequest;
import com.monovai.domain.payment.dto.request.RedeemPromotionRequest;
import com.monovai.domain.payment.entity.PromotionCode;
import com.monovai.domain.payment.entity.enums.PromotionStatus;
import com.monovai.domain.payment.repository.PromotionCodeRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final PromotionCodeRepository promotionRepository;
	private final CreditService creditService;

	@Transactional
	public Map<String, Object> create(CreatePromotionRequest request) {
		String code = generateUniqueCode();
		LocalDateTime expiresAt = request.expiresInDays() != null
			? LocalDateTime.now().plusDays(request.expiresInDays()) : null;
		PromotionCode promo = PromotionCode.create(code, request.issuedTo(), request.credits(), expiresAt);
		promotionRepository.save(promo);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("code", code);
		body.put("credits", request.credits());
		return body;
	}

	@Transactional
	public Map<String, Object> redeem(Long userId, RedeemPromotionRequest request) {
		String code = request.code() == null ? "" : request.code().trim().toUpperCase();
		PromotionCode promo = promotionRepository.findByCode(code)
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROMO_CODE_NOT_FOUND));

		if (promo.getStatus() == PromotionStatus.REDEEMED) {
			throw new BadRequestException(ErrorCode.PROMO_CODE_ALREADY_REDEEMED);
		}
		if (promo.isExpired()) {
			throw new BadRequestException(ErrorCode.PROMO_CODE_EXPIRED);
		}
		if (promo.getIssuedTo() != null && !promo.getIssuedTo().equals(userId)) {
			throw new BadRequestException(ErrorCode.PROMO_CODE_INVALID);
		}

		promo.redeem(userId);
		int balance = creditService.grant(userId, CreditTxnType.GRANT_PROMO, promo.getCredits(),
			"프로모션 코드: " + code, "promo:" + code);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("grantedCredits", promo.getCredits());
		body.put("balance", balance);
		return body;
	}

	private String generateUniqueCode() {
		for (int attempt = 0; attempt < 10; attempt++) {
			StringBuilder sb = new StringBuilder(8);
			for (int i = 0; i < 8; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
			String code = sb.toString();
			if (promotionRepository.findById(code).isEmpty()) return code;
		}
		throw new BadRequestException(ErrorCode.PROMO_CODE_INVALID);
	}
}
