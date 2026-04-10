package com.monovai.domain.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class UserOnboarding {

	@Id
	@GeneratedValue
	private Long id;

	private Long userId;   // FK (단방향 추천)

	private String job;
	private String source;

	private LocalDateTime completedAt;

	@Builder
	public UserOnboarding(Long userId, String job, String source, LocalDateTime completedAt) {
		this.userId = userId;
		this.job = job;
		this.source = source;
		this.completedAt = completedAt;
	}
}
