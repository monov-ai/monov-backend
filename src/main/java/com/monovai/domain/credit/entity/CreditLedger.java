package com.monovai.domain.credit.entity;

import com.monovai.domain.credit.entity.enums.CreditTxnType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 크레딧 변동 원장. (jobId, type) UNIQUE 로 멱등성 보장 — 같은 잡에 대해 중복 차감/환불 방지.
 */
@Entity
@Table(name = "credit_ledgers",
	uniqueConstraints = @UniqueConstraint(name = "uk_credit_ledger_job_type", columnNames = {"job_id", "type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditLedger extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CreditTxnType type;

	/** signed: 차감은 음수, 충전은 양수. */
	@Column(nullable = false)
	private int amount;

	@Column(nullable = false)
	private int balanceAfter;

	@Column(length = 300)
	private String reason;

	/** 멱등키. null 이면 멱등 제약 없음 (단, UNIQUE 가 NULL 다중 허용). */
	@Column(name = "job_id", length = 100)
	private String jobId;

	@Builder(access = AccessLevel.PRIVATE)
	private CreditLedger(User user, CreditTxnType type, int amount, int balanceAfter, String reason, String jobId) {
		this.user = user;
		this.type = type;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.reason = reason;
		this.jobId = jobId;
	}

	public static CreditLedger create(User user, CreditTxnType type, int amount, int balanceAfter,
		String reason, String jobId) {
		return CreditLedger.builder()
			.user(user).type(type).amount(amount).balanceAfter(balanceAfter).reason(reason).jobId(jobId)
			.build();
	}
}
