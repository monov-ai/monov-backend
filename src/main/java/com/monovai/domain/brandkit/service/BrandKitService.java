package com.monovai.domain.brandkit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.brandkit.entity.BrandKit;
import com.monovai.domain.brandkit.entity.BrandKitAccount;
import com.monovai.domain.brandkit.entity.BrandKitPlan;
import com.monovai.domain.brandkit.repository.BrandKitAccountRepository;
import com.monovai.domain.brandkit.repository.BrandKitPlanRepository;
import com.monovai.domain.brandkit.repository.BrandKitRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandKitService {

	private final BrandKitRepository brandKitRepository;
	private final BrandKitAccountRepository accountRepository;
	private final BrandKitPlanRepository planRepository;
	private final UserRepository userRepository;

	public Object getBrandKit(Long userId) {
		return brandKitRepository.findByUser_Id(userId).map(BrandKit::getInfo).orElse(null);
	}

	@Transactional
	public Object upsertBrandKit(Long userId, Object info) {
		BrandKit kit = brandKitRepository.findByUser_Id(userId).orElse(null);
		if (kit == null) {
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
			kit = brandKitRepository.save(BrandKit.create(user, info));
		} else {
			kit.updateInfo(info);
		}
		return kit.getInfo();
	}

	public List<BrandKitAccount> listAccounts(Long userId) {
		return accountRepository.findAllByUserId(userId);
	}

	@Transactional
	public BrandKitAccount createAccount(Long userId, Object data) {
		String accountId = "acc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		return accountRepository.save(BrandKitAccount.create(userId, accountId, data));
	}

	@Transactional
	public BrandKitAccount updateAccount(Long userId, String accountId, Object data) {
		BrandKitAccount acc = accountRepository.findByUserIdAndAccountId(userId, accountId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_KIT_NOT_FOUND));
		acc.updateData(data);
		return acc;
	}

	@Transactional
	public void deleteAccount(Long userId, String accountId) {
		BrandKitAccount acc = accountRepository.findByUserIdAndAccountId(userId, accountId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_KIT_NOT_FOUND));
		accountRepository.delete(acc);
	}

	public Object getPlan(Long userId, String weekId) {
		return planRepository.findByUserIdAndWeekId(userId, weekId).map(BrandKitPlan::getData).orElse(null);
	}

	@Transactional
	public Object upsertPlan(Long userId, String weekId, Object data) {
		BrandKitPlan plan = planRepository.findByUserIdAndWeekId(userId, weekId).orElse(null);
		if (plan == null) {
			plan = planRepository.save(BrandKitPlan.create(userId, weekId, data));
		} else {
			plan.updateData(data);
		}
		return plan.getData();
	}
}
