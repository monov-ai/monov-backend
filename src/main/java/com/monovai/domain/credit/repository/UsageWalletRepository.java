package com.monovai.domain.credit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.credit.entity.UsageWallet;

public interface UsageWalletRepository extends JpaRepository<UsageWallet, Long> {
	Optional<UsageWallet> findByUser_Id(Long userId);
}
