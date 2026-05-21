package com.monovai.domain.brandkit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.brandkit.entity.BrandKitAccount;

public interface BrandKitAccountRepository extends JpaRepository<BrandKitAccount, Long> {
	List<BrandKitAccount> findAllByUserId(Long userId);

	Optional<BrandKitAccount> findByUserIdAndAccountId(Long userId, String accountId);
}
