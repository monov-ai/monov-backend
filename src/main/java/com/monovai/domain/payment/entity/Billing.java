package com.monovai.domain.payment.entity;

import com.monovai.domain.user.entity.User;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Toss 빌링키. 정기결제용. 카드 정보는 마스킹된 메타만 보관.
 */
@Entity
@Table(name = "billings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Billing extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 200)
	private String billingKey;

	@Column(nullable = false, length = 200)
	private String customerKey;

	@Column(length = 10)
	private String cardLast4;

	@Column(length = 30)
	private String cardBrand;

	@Column(length = 50)
	private String planId;

	@Column(nullable = false)
	private boolean active;

	@Builder(access = AccessLevel.PRIVATE)
	private Billing(User user, String billingKey, String customerKey, String cardLast4, String cardBrand,
		String planId) {
		this.user = user;
		this.billingKey = billingKey;
		this.customerKey = customerKey;
		this.cardLast4 = cardLast4;
		this.cardBrand = cardBrand;
		this.planId = planId;
		this.active = true;
	}

	public static Billing create(User user, String billingKey, String customerKey,
		String cardLast4, String cardBrand, String planId) {
		return Billing.builder()
			.user(user).billingKey(billingKey).customerKey(customerKey)
			.cardLast4(cardLast4).cardBrand(cardBrand).planId(planId)
			.build();
	}

	public void deactivate() {
		this.active = false;
	}
}
