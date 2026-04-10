package com.monovai.domain.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(columnDefinition = "TEXT")
	private String picture;
	private String provider;

	private Integer credits;

	private String job;
	private String source;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private LocalDateTime onboardingCompletedAt;

	@Builder
	public User(String uid, String email, String name, String picture, String provider, Integer credits, String job,
		String source, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime onboardingCompletedAt) {
		this.uid = uid;
		this.email = email;
		this.name = name;
		this.picture = picture;
		this.provider = provider;
		this.credits = credits;
		this.job = job;
		this.source = source;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.onboardingCompletedAt = onboardingCompletedAt;
	}
}
