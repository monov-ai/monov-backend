package com.monovai.domain.payment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.monovai.domain.payment.entity.enums.SubscriptionStatus;
import com.monovai.domain.user.entity.User;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 50)
	private String planId;

	@Column(length = 100)
	private String planName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriptionStatus status;

	private LocalDateTime currentPeriodStart;
	private LocalDateTime currentPeriodEnd;
	private LocalDate nextBillingDate;
	private LocalDateTime cancelledAt;

	@Column(length = 200)
	private String billingKey;

	@Column(length = 200)
	private String customerKey;

	private int retryCount;

	// 월 정액 (스냅샷)
	private int monthlyLegacyCredits;
	private int monthlyAdImage;
	private int monthlyAiVideo;
	private int monthlyImageToVideo;

	@Builder(access = AccessLevel.PRIVATE)
	private Subscription(User user, String planId, String planName, SubscriptionStatus status,
		LocalDateTime currentPeriodStart, LocalDateTime currentPeriodEnd, LocalDate nextBillingDate,
		String billingKey, String customerKey,
		int monthlyLegacyCredits, int monthlyAdImage, int monthlyAiVideo, int monthlyImageToVideo) {
		this.user = user;
		this.planId = planId;
		this.planName = planName;
		this.status = status;
		this.currentPeriodStart = currentPeriodStart;
		this.currentPeriodEnd = currentPeriodEnd;
		this.nextBillingDate = nextBillingDate;
		this.billingKey = billingKey;
		this.customerKey = customerKey;
		this.monthlyLegacyCredits = monthlyLegacyCredits;
		this.monthlyAdImage = monthlyAdImage;
		this.monthlyAiVideo = monthlyAiVideo;
		this.monthlyImageToVideo = monthlyImageToVideo;
	}

	public static Subscription create(User user, String planId, String planName,
		String billingKey, String customerKey,
		int monthlyLegacyCredits, int monthlyAdImage, int monthlyAiVideo, int monthlyImageToVideo) {
		LocalDateTime now = LocalDateTime.now();
		return Subscription.builder()
			.user(user).planId(planId).planName(planName)
			.status(SubscriptionStatus.ACTIVE)
			.currentPeriodStart(now)
			.currentPeriodEnd(now.plusMonths(1))
			.nextBillingDate(LocalDate.now().plusMonths(1))
			.billingKey(billingKey).customerKey(customerKey)
			.monthlyLegacyCredits(monthlyLegacyCredits).monthlyAdImage(monthlyAdImage)
			.monthlyAiVideo(monthlyAiVideo).monthlyImageToVideo(monthlyImageToVideo)
			.build();
	}

	public void renew() {
		LocalDateTime now = LocalDateTime.now();
		this.status = SubscriptionStatus.ACTIVE;
		this.currentPeriodStart = now;
		this.currentPeriodEnd = now.plusMonths(1);
		this.nextBillingDate = this.nextBillingDate.plusMonths(1);
		this.retryCount = 0;
	}

	public void markPastDue() {
		this.status = SubscriptionStatus.PAST_DUE;
		this.retryCount += 1;
	}

	public void cancel() {
		this.status = SubscriptionStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
	}

	public boolean isActive() {
		return this.status == SubscriptionStatus.ACTIVE;
	}
}
