package com.monovai.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.payment.entity.Billing;

public interface BillingRepository extends JpaRepository<Billing, Long> {

	Optional<Billing> findFirstByUser_IdAndActiveTrueOrderByCreatedAtDesc(Long userId);

	Optional<Billing> findByBillingKey(String billingKey);
}
