package com.monovai.domain.payment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.payment.entity.Subscription;
import com.monovai.domain.payment.entity.enums.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findFirstByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, SubscriptionStatus status);

	Optional<Subscription> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

	List<Subscription> findAllByStatusAndNextBillingDateLessThanEqual(SubscriptionStatus status, LocalDate date);
}
