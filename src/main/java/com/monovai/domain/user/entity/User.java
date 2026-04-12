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

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Column(name = "social_type")
	@Enumerated(EnumType.STRING)
	private SocialType socialType;

	@Column(name = "social_id")
	private String socialId;

	@Enumerated(EnumType.STRING)
	private Role role;

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
}
