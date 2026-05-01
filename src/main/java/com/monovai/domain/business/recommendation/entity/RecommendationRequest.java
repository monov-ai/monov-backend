package com.monovai.domain.business.recommendation.entity;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.business.recommendation.entity.enums.RecommendationStatus;
import com.monovai.domain.business.recommendation.entity.enums.Style;
import com.monovai.domain.business.recommendation.entity.value.CorePoints;
import com.monovai.domain.business.recommendation.entity.value.RecommendationItem;
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
@Table(name = "recommendation_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationRequest extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, length = 50)
	private String requestSlug;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Style style;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String productImageUrl;

	@Column(columnDefinition = "TEXT")
	private String productImagePath;

	@Column(columnDefinition = "TEXT")
	private String referenceImageUrl;

	@Column(columnDefinition = "TEXT")
	private String referenceImagePath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RecommendationStatus status;

	@Column(columnDefinition = "TEXT")
	private String headline;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private CorePoints corePoints;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<RecommendationItem> recommendations;

	@Builder(access = AccessLevel.PRIVATE)
	private RecommendationRequest(
		String requestSlug,
		User user,
		Style style,
		String description,
		String productImageUrl,
		String productImagePath,
		String referenceImageUrl,
		String referenceImagePath,
		RecommendationStatus status
	) {
		this.requestSlug = requestSlug;
		this.user = user;
		this.style = style;
		this.description = description;
		this.productImageUrl = productImageUrl;
		this.productImagePath = productImagePath;
		this.referenceImageUrl = referenceImageUrl;
		this.referenceImagePath = referenceImagePath;
		this.status = status;
	}

	public static RecommendationRequest create(
		String requestSlug,
		User user,
		Style style,
		String description,
		String productImageUrl,
		String productImagePath,
		String referenceImageUrl,
		String referenceImagePath
	) {
		return RecommendationRequest.builder()
			.requestSlug(requestSlug)
			.user(user)
			.style(style)
			.description(description)
			.productImageUrl(productImageUrl)
			.productImagePath(productImagePath)
			.referenceImageUrl(referenceImageUrl)
			.referenceImagePath(referenceImagePath)
			.status(RecommendationStatus.PENDING)
			.build();
	}

	public void markRunning() {
		this.status = RecommendationStatus.RUNNING;
	}

	public void completeWith(String headline, String summary, CorePoints corePoints,
		List<RecommendationItem> recommendations) {
		this.headline = headline;
		this.summary = summary;
		this.corePoints = corePoints;
		this.recommendations = recommendations;
		this.status = RecommendationStatus.COMPLETED;
	}

	public void markFailed() {
		this.status = RecommendationStatus.FAILED;
	}
}
