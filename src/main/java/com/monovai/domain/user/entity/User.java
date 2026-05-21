package com.monovai.domain.user.entity;

import java.time.LocalDateTime;

import com.monovai.domain.auth.entity.enums.SocialType;
import com.monovai.domain.user.entity.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String uid; //firebase uid
	private String email;
	private String name;
	private String nickname;

	@Column(columnDefinition = "TEXT")
	private String picture;
	private String provider;

	private Integer credits;

	private String job;
	private String source;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private LocalDateTime onboardingCompletedAt;

	// A.1 온보딩 (직군 / 유입경로)
	@Column(name = "onboarding_job")
	private String onboardingJob;

	@Column(name = "onboarding_source")
	private String onboardingSource;

	// A.2 사용자 선호 locale (ko | en | ja)
	@Column(name = "locale", length = 5)
	private String locale;

	// A.3 동의 메타데이터
	@Column(name = "terms_accepted")
	private Boolean termsAccepted;

	@Column(name = "privacy_accepted")
	private Boolean privacyAccepted;

	@Column(name = "content_usage_accepted")
	private Boolean contentUsageAccepted;

	@Column(name = "marketing_accepted")
	private Boolean marketingAccepted;

	@Column(name = "consent_version", length = 20)
	private String consentVersion;

	@Column(name = "consent_at")
	private LocalDateTime consentAt;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Column(name = "social_type")
	@Enumerated(EnumType.STRING)
	private SocialType socialType;

	@Column(name = "social_id")
	private String socialId;

	@Enumerated(EnumType.STRING)
	private Role role;

	@Column(name = "active")
	private Boolean active;

	@Builder
	public User(String email, String name, String nickname, String picture, String provider, Integer credits,
		String job,
		String source, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime onboardingCompletedAt,
		String profileImageUrl, SocialType socialType, String socialId, Role role) {
		this.email = email;
		this.name = name;
		this.nickname = nickname;
		this.picture = picture;
		this.provider = provider;
		this.credits = credits;
		this.job = job;
		this.source = source;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.onboardingCompletedAt = onboardingCompletedAt;
		this.profileImageUrl = profileImageUrl;
		this.socialType = socialType;
		this.socialId = socialId;
		this.role = role;
	}

	public void completeOnboarding(String job, String source, LocalDateTime completedAt) {
		this.onboardingJob = job;
		this.onboardingSource = source;
		this.onboardingCompletedAt = completedAt;
	}

	public void updateLocale(String locale) {
		this.locale = locale;
	}

	public void recordConsents(boolean terms, boolean privacy, boolean contentUsage, boolean marketing,
		String version, LocalDateTime at) {
		this.termsAccepted = terms;
		this.privacyAccepted = privacy;
		this.contentUsageAccepted = contentUsage;
		this.marketingAccepted = marketing;
		this.consentVersion = version;
		this.consentAt = at;
	}

	public boolean isOnboardingCompleted() {
		return this.onboardingCompletedAt != null;
	}

	public void deactivate() {
		this.active = false;
	}
}
