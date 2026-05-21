package com.monovai.domain.user.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.credit.CreditPolicy;
import com.monovai.domain.credit.entity.enums.CreditTxnType;
import com.monovai.domain.credit.service.CreditService;
import com.monovai.domain.user.dto.request.ConsentRequest;
import com.monovai.domain.user.dto.request.LocaleRequest;
import com.monovai.domain.user.dto.request.OnboardingRequest;
import com.monovai.domain.user.dto.response.OnboardingResponse;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserMeService {

	private static final Set<String> ALLOWED_LOCALES = Set.of("ko", "en", "ja");

	private final UserRepository userRepository;
	private final CreditService creditService;

	public OnboardingResponse getOnboarding(Long userId) {
		return OnboardingResponse.fetched(getUser(userId));
	}

	@Transactional
	public OnboardingResponse completeOnboarding(Long userId, OnboardingRequest request) {
		if (request.job() == null || request.job().isBlank()
			|| request.source() == null || request.source().isBlank()) {
			throw new BadRequestException(ErrorCode.INVALID_ONBOARDING);
		}
		User user = getUser(userId);
		user.completeOnboarding(request.job(), request.source(), LocalDateTime.now());
		return OnboardingResponse.posted(user);
	}

	public String getLocale(Long userId) {
		String l = getUser(userId).getLocale();
		return l == null ? "ko" : l;
	}

	@Transactional
	public String updateLocale(Long userId, LocaleRequest request) {
		String locale = request.locale() == null ? null : request.locale().toLowerCase();
		if (!ALLOWED_LOCALES.contains(locale)) {
			throw new BadRequestException(ErrorCode.INVALID_LOCALE);
		}
		User user = getUser(userId);
		user.updateLocale(locale);
		return locale;
	}

	/**
	 * 동의 메타 기록 + 가입 보너스 80cr 충전 (멱등: jobId = "signup_bonus:{userId}").
	 */
	@Transactional
	public int recordConsents(Long userId, ConsentRequest request) {
		if (!Boolean.TRUE.equals(request.termsAccepted())
			|| !Boolean.TRUE.equals(request.privacyAccepted())
			|| !Boolean.TRUE.equals(request.contentUsageAccepted())) {
			throw new BadRequestException(ErrorCode.CONSENT_REQUIRED);
		}
		User user = getUser(userId);
		user.recordConsents(
			true, true, true,
			Boolean.TRUE.equals(request.marketingAccepted()),
			request.version(),
			LocalDateTime.now()
		);
		creditService.grant(
			userId, CreditTxnType.GRANT_SIGNUP, CreditPolicy.SIGNUP_BONUS,
			"가입 보너스 크레딧", "signup_bonus:" + userId
		);
		return CreditPolicy.SIGNUP_BONUS;
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
	}
}
