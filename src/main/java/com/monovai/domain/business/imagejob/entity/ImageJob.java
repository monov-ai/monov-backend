package com.monovai.domain.business.imagejob.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.business.imagejob.entity.enums.Angle;
import com.monovai.domain.business.imagejob.entity.enums.JobStatus;
import com.monovai.domain.business.imagejob.entity.enums.Lighting;
import com.monovai.domain.business.imagejob.entity.enums.Ratio;
import com.monovai.domain.business.recommendation.entity.RecommendationRequest;
import com.monovai.domain.business.recommendation.entity.enums.Style;
import com.monovai.domain.business.recommendation.entity.value.CorePoints;
import com.monovai.domain.user.entity.User;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

	@Column(unique = true, nullable = false, length = 50)
	private String jobSlug;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "request_id", nullable = false)
	private RecommendationRequest request;

	// 추천 요청에서 스냅샷한 공통 입력 (snapshot semantics)
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

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private CorePoints corePoints;

	// 사용자가 이번 잡에서 선택한 옵션
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Angle angle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Lighting lighting;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Ratio ratio;

	// 잡 진행 상태
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JobStatus status;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("variantSeq ASC")
	private List<ImageJobVariant> variants = new ArrayList<>();

	@Builder(access = AccessLevel.PRIVATE)
	private ImageJob(
		String jobSlug, User user, RecommendationRequest request,
		Style style, String description,
		String productImageUrl, String productImagePath, String referenceImageUrl,
		CorePoints corePoints,
		Angle angle, Lighting lighting, Ratio ratio,
		JobStatus status
	) {
		this.jobSlug = jobSlug;
		this.user = user;
		this.request = request;
		this.style = style;
		this.description = description;
		this.productImageUrl = productImageUrl;
		this.productImagePath = productImagePath;
		this.referenceImageUrl = referenceImageUrl;
		this.corePoints = corePoints;
		this.angle = angle;
		this.lighting = lighting;
		this.ratio = ratio;
		this.status = status;
	}

	public static ImageJob create(
		String jobSlug, User user, RecommendationRequest request,
		Angle angle, Lighting lighting, Ratio ratio
	) {
		return ImageJob.builder()
			.jobSlug(jobSlug)
			.user(user)
			.request(request)
			// 추천 요청 정보 스냅샷
			.style(request.getStyle())
			.description(request.getDescription())
			.productImageUrl(request.getProductImageUrl())
			.productImagePath(request.getProductImagePath())
			.referenceImageUrl(request.getReferenceImageUrl())
			.corePoints(request.getCorePoints())
			// 사용자 옵션
			.angle(angle)
			.lighting(lighting)
			.ratio(ratio)
			// 초기 상태
			.status(JobStatus.PENDING)
			.build();
	}

	public void addVariant(ImageJobVariant variant) {
		this.variants.add(variant);
	}

	public void markRunning() {
		this.status = JobStatus.RUNNING;
	}

	public void markCompleted() {
		this.status = JobStatus.COMPLETED;
	}

	public void markPartial() {
		this.status = JobStatus.PARTIAL;
	}

	public void markFailed(String errorMessage) {
		this.status = JobStatus.FAILED;
		this.errorMessage = errorMessage;
	}

	/**
	 * 모든 variant 처리 후 잡 전체 상태 결정.
	 */
	public void finalizeStatus() {
		long total = variants.size();
		long succeeded = variants.stream().filter(v -> v.getStatus().name().equals("COMPLETED")).count();
		long failed = variants.stream().filter(v -> v.getStatus().name().equals("FAILED")).count();

		if (succeeded == total) {
			this.status = JobStatus.COMPLETED;
		} else if (failed == total) {
			this.status = JobStatus.FAILED;
		} else if (succeeded > 0) {
			this.status = JobStatus.PARTIAL;
		} else {
			this.status = JobStatus.FAILED;
		}
	}
}