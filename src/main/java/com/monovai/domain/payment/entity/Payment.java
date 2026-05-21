package com.monovai.domain.payment.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.payment.entity.enums.PaymentType;
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
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(length = 200)
	private String paymentKey;

	@Column(length = 100)
	private String orderId;

	@Column(nullable = false)
	private int amount;

	@Column(length = 50)
	private String packageId;

	@Column(length = 50)
	private String planId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentType type;

	@Column(length = 30)
	private String status;

	@Column(length = 100)
	private String statusDetail;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private Object raw;

	@Builder(access = AccessLevel.PRIVATE)
	private Payment(User user, String paymentKey, String orderId, int amount, String packageId, String planId,
		PaymentType type, String status, String statusDetail, Object raw) {
		this.user = user;
		this.paymentKey = paymentKey;
		this.orderId = orderId;
		this.amount = amount;
		this.packageId = packageId;
		this.planId = planId;
		this.type = type;
		this.status = status;
		this.statusDetail = statusDetail;
		this.raw = raw;
	}

	public static Payment create(User user, String paymentKey, String orderId, int amount,
		String packageId, String planId, PaymentType type, String status, String statusDetail, Object raw) {
		return Payment.builder()
			.user(user).paymentKey(paymentKey).orderId(orderId).amount(amount)
			.packageId(packageId).planId(planId).type(type).status(status).statusDetail(statusDetail).raw(raw)
			.build();
	}
}
