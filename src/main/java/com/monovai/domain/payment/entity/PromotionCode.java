package com.monovai.domain.payment.entity;

import java.time.LocalDateTime;

import com.monovai.domain.payment.entity.enums.PromotionStatus;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionCode extends BaseTimeEntity {

	/** 8자리 영문대문자 코드 (PK). */
	@Id
	@Column(length = 8)
	private String code;

	/** 발급 대상 사용자 (null 이면 누구나 1회). */
	private Long issuedTo;

	/** 사용한 사용자. */
	private Long redeemedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PromotionStatus status;

	@Column(nullable = false)
	private int credits;

	private LocalDateTime redeemedAt;
	private LocalDateTime expiresAt;

	@Builder(access = AccessLevel.PRIVATE)
	private PromotionCode(String code, Long issuedTo, int credits, LocalDateTime expiresAt) {
		this.code = code;
		this.issuedTo = issuedTo;
		this.credits = credits;
		this.expiresAt = expiresAt;
		this.status = PromotionStatus.ACTIVE;
	}

	public static PromotionCode create(String code, Long issuedTo, int credits, LocalDateTime expiresAt) {
		return PromotionCode.builder().code(code).issuedTo(issuedTo).credits(credits).expiresAt(expiresAt).build();
	}

	public void redeem(Long userId) {
		this.status = PromotionStatus.REDEEMED;
		this.redeemedBy = userId;
		this.redeemedAt = LocalDateTime.now();
	}

	public boolean isExpired() {
		return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
	}
}
