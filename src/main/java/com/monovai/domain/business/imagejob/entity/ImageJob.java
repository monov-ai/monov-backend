package com.monovai.domain.business.imagejob.entity;

import com.monovai.domain.business.imagejob.entity.enums.JobStatus;
import com.monovai.domain.business.recommendation.entity.RecommendationRequest;
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
@Table(name = "image_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageJob extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "request_id", nullable = false)
	private RecommendationRequest request;

	@Column(columnDefinition = "TEXT")
	private String optionsJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JobStatus status;

	@Column(columnDefinition = "TEXT")
	private String resultImageUrl;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Builder(access = AccessLevel.PRIVATE)
	private ImageJob(User user, RecommendationRequest request, String optionsJson, JobStatus status) {
		this.user = user;
		this.request = request;
		this.optionsJson = optionsJson;
		this.status = status;
	}

	public static ImageJob create(User user, RecommendationRequest request, String optionsJson) {
		return ImageJob.builder()
			.user(user)
			.request(request)
			.optionsJson(optionsJson)
			.status(JobStatus.PENDING)
			.build();
	}

	public void markRunning() {
		this.status = JobStatus.RUNNING;
	}

	public void markSucceeded(String resultImageUrl) {
		this.status = JobStatus.SUCCEEDED;
		this.resultImageUrl = resultImageUrl;
	}

	public void markFailed(String errorMessage) {
		this.status = JobStatus.FAILED;
		this.errorMessage = errorMessage;
	}
}