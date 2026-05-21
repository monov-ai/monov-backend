package com.monovai.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	boolean existsByPaymentKey(String paymentKey);

	Optional<Payment> findByOrderId(String orderId);
}
