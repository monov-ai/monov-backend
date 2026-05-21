package com.monovai.domain.credit.entity;

import com.monovai.domain.user.entity.User;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 사용자 1인당 1개의 사용 지갑. 카테고리별 잔여 + 통합 레거시 크레딧.
 */
@Entity
@Table(name = "usage_wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageWallet extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(nullable = false)
	private int adImage;

	@Column(nullable = false)
	private int aiVideo;

	@Column(nullable = false)
	private int imageToVideo;

	/** 통합 크레딧 풀 (실제 차감/충전 단위). */
	@Column(nullable = false)
	private int legacyCredits;

	@Builder(access = AccessLevel.PRIVATE)
	private UsageWallet(User user, int adImage, int aiVideo, int imageToVideo, int legacyCredits) {
		this.user = user;
		this.adImage = adImage;
		this.aiVideo = aiVideo;
		this.imageToVideo = imageToVideo;
		this.legacyCredits = legacyCredits;
	}

	public static UsageWallet createEmpty(User user) {
		return UsageWallet.builder().user(user).adImage(0).aiVideo(0).imageToVideo(0).legacyCredits(0).build();
	}

	public void addLegacyCredits(int delta) {
		this.legacyCredits += delta;
	}

	public void deductLegacyCredits(int amount) {
		this.legacyCredits -= amount;
	}

	public void adjust(int deltaLegacy, int deltaAdImage, int deltaAiVideo, int deltaImageToVideo) {
		this.legacyCredits += deltaLegacy;
		this.adImage += deltaAdImage;
		this.aiVideo += deltaAiVideo;
		this.imageToVideo += deltaImageToVideo;
	}

	/** 구독 갱신 시 월 정액 리셋 + 충전. */
	public void resetMonthlyAllowance(int legacy, int adImage, int aiVideo, int imageToVideo) {
		this.legacyCredits = legacy;
		this.adImage = adImage;
		this.aiVideo = aiVideo;
		this.imageToVideo = imageToVideo;
	}
}
