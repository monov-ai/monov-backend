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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brand_kit_plans",
	uniqueConstraints = @UniqueConstraint(name = "uk_brandkit_plan_user_week", columnNames = {"user_id", "week_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandKitPlan extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "week_id", nullable = false, length = 20)
	private String weekId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private Object data;

	@Builder(access = AccessLevel.PRIVATE)
	private BrandKitPlan(Long userId, String weekId, Object data) {
		this.userId = userId;
		this.weekId = weekId;
		this.data = data;
	}

	public static BrandKitPlan create(Long userId, String weekId, Object data) {
		return BrandKitPlan.builder().userId(userId).weekId(weekId).data(data).build();
	}

	public void updateData(Object data) {
		this.data = data;
	}
}
