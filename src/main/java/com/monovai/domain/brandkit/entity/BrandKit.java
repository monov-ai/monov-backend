package com.monovai.domain.brandkit.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.user.entity.User;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * C.3 Studio 마케팅 자동화 브랜드 키트 (비즈니스 brand 와 별개). 사용자당 1개.
 * info 는 Firestore 의 느슨한 스키마를 그대로 수용하는 JSON blob.
 */
@Entity
@Table(name = "brand_kits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandKit extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private Object info;

	@Builder(access = AccessLevel.PRIVATE)
	private BrandKit(User user, Object info) {
		this.user = user;
		this.info = info;
	}

	public static BrandKit create(User user, Object info) {
		return BrandKit.builder().user(user).info(info).build();
	}

	public void updateInfo(Object info) {
		this.info = info;
	}
}
