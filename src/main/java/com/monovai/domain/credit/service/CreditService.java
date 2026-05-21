package com.monovai.domain.credit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.credit.entity.CreditLedger;
import com.monovai.domain.credit.entity.UsageWallet;
import com.monovai.domain.credit.entity.enums.CreditTxnType;
import com.monovai.domain.credit.repository.CreditLedgerRepository;
import com.monovai.domain.credit.repository.UsageWalletRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 크레딧 내부 서비스. 차감/충전/환불은 서비스 레이어 호출로만 (외부 API 노출 X).
 * 멱등성: (jobId, type) UNIQUE — 같은 잡 중복 차감/환불 방지.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CreditService {

	private final UsageWalletRepository walletRepository;
	private final CreditLedgerRepository ledgerRepository;
	private final UserRepository userRepository;

	@Transactional
	public UsageWallet getOrCreateWallet(Long userId) {
		return walletRepository.findByUser_Id(userId).orElseGet(() -> {
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
			return walletRepository.save(UsageWallet.createEmpty(user));
		});
	}

	public UsageWallet getWallet(Long userId) {
		return walletRepository.findByUser_Id(userId).orElse(null);
	}

	/**
	 * 크레딧 차감. 멱등키(jobId) 가 이미 차감됐으면 no-op.
	 * 잔액 부족 시 INSUFFICIENT_CREDIT.
	 */
	@Transactional
	public void deduct(Long userId, int amount, String reason, String jobId) {
		if (amount <= 0) return;
		if (jobId != null && ledgerRepository.existsByJobIdAndType(jobId, CreditTxnType.DEDUCT)) {
			log.info("[Credit] 멱등 차감 스킵: userId={} jobId={}", userId, jobId);
			return;
		}
		UsageWallet wallet = getOrCreateWallet(userId);
		if (wallet.getLegacyCredits() < amount) {
			throw new BusinessException(ErrorCode.INSUFFICIENT_CREDIT);
		}
		wallet.deductLegacyCredits(amount);
		writeLedger(wallet.getUser(), CreditTxnType.DEDUCT, -amount, wallet.getLegacyCredits(), reason, jobId);
	}

	/**
	 * 환불. 멱등키 동일하면 no-op. 워커 실패 시 같은 트랜잭션에서 호출.
	 */
	@Transactional
	public void refund(Long userId, int amount, String reason, String jobId) {
		if (amount <= 0) return;
		if (jobId != null && ledgerRepository.existsByJobIdAndType(jobId, CreditTxnType.REFUND)) {
			log.info("[Credit] 멱등 환불 스킵: userId={} jobId={}", userId, jobId);
			return;
		}
		UsageWallet wallet = getOrCreateWallet(userId);
		wallet.addLegacyCredits(amount);
		writeLedger(wallet.getUser(), CreditTxnType.REFUND, amount, wallet.getLegacyCredits(), reason, jobId);
	}

	@Transactional
	public int grant(Long userId, CreditTxnType type, int amount, String reason, String jobId) {
		if (amount <= 0) return getOrCreateWallet(userId).getLegacyCredits();
		if (jobId != null && ledgerRepository.existsByJobIdAndType(jobId, type)) {
			log.info("[Credit] 멱등 충전 스킵: userId={} type={} jobId={}", userId, type, jobId);
			return getOrCreateWallet(userId).getLegacyCredits();
		}
		UsageWallet wallet = getOrCreateWallet(userId);
		wallet.addLegacyCredits(amount);
		writeLedger(wallet.getUser(), type, amount, wallet.getLegacyCredits(), reason, jobId);
		return wallet.getLegacyCredits();
	}

	@Transactional
	public void adjust(Long userId, int deltaLegacy, int deltaAdImage, int deltaAiVideo, int deltaImageToVideo,
		String reason) {
		UsageWallet wallet = getOrCreateWallet(userId);
		wallet.adjust(deltaLegacy, deltaAdImage, deltaAiVideo, deltaImageToVideo);
		writeLedger(wallet.getUser(), CreditTxnType.ADJUST, deltaLegacy, wallet.getLegacyCredits(), reason, null);
	}

	private void writeLedger(User user, CreditTxnType type, int amount, int balanceAfter, String reason, String jobId) {
		ledgerRepository.save(CreditLedger.create(user, type, amount, balanceAfter, reason, jobId));
	}
}
