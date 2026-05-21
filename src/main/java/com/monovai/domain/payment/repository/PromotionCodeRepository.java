package com.monovai.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.monovai.domain.payment.entity.PromotionCode;

import jakarta.persistence.LockModeType;

public interface PromotionCodeRepository extends JpaRepository<PromotionCode, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PromotionCode> findByCode(String code);
}
