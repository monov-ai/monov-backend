package com.monovai.domain.brandkit.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brand_kit_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandKitAccount extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 50)
	private String accountId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private Object data;

	@Builder(access = AccessLevel.PRIVATE)
	private BrandKitAccount(Long userId, String accountId, Object data) {
		this.userId = userId;
		this.accountId = accountId;
		this.data = data;
	}

	public static BrandKitAccount create(Long userId, String accountId, Object data) {
		return BrandKitAccount.builder().userId(userId).accountId(accountId).data(data).build();
	}

	public void updateData(Object data) {
		this.data = data;
	}
}
