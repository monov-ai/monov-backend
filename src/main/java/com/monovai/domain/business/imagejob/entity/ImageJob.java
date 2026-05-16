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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Style style;

	@Column(columnDefinition = "TEXT")
	private String description;

	// v1 호환: 단수형 (배열 첫 번째)
	@Column(columnDefinition = "TEXT")
	private String productImageUrl;

	@Column(columnDefinition = "TEXT")
	private String productImagePath;

	@Column(columnDefinition = "TEXT")
	private String referenceImageUrl;

	// v2.0: 다중 이미지 스냅샷
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> productImageUrls;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> productImagePaths;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> referenceImageUrls;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> referenceImagePaths;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private CorePoints corePoints;

	// v2.0: angle 은 선택값 (deprecated). 추천 단계에서 받지 않음.
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Angle angle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Lighting lighting;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Ratio ratio;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JobStatus status;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("variantSeq ASC")
	private List<ImageJobVariant> variants = new ArrayList<>();

	// v2.0: 즐겨찾기 (variantId 또는 editSlug 문자열 배열)
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> favoriteVariantIds = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> favoriteEditIds = new ArrayList<>();

	@Builder(access = AccessLevel.PRIVATE)
	private ImageJob(
		String jobSlug, User user, RecommendationRequest request,
		Style style, String description,
		String productImageUrl, String productImagePath, String referenceImageUrl,
		List<String> productImageUrls, List<String> productImagePaths,
		List<String> referenceImageUrls, List<String> referenceImagePaths,
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
		this.productImageUrls = productImageUrls;
		this.productImagePaths = productImagePaths;
		this.referenceImageUrls = referenceImageUrls;
		this.referenceImagePaths = referenceImagePaths;
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
			.style(request.getStyle())
			.description(request.getDescription())
			.productImageUrl(request.getProductImageUrl())
			.productImagePath(request.getProductImagePath())
			.referenceImageUrl(request.getReferenceImageUrl())
			.productImageUrls(request.getProductImageUrls())
			.productImagePaths(request.getProductImagePaths())
			.referenceImageUrls(request.getReferenceImageUrls())
			.referenceImagePaths(request.getReferenceImagePaths())
			.corePoints(request.getCorePoints())
			.angle(angle)
			.lighting(lighting)
			.ratio(ratio)
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

	public void setFavoriteVariant(String variantId, boolean favorite) {
		if (this.favoriteVariantIds == null) this.favoriteVariantIds = new ArrayList<>();
		if (favorite) {
			if (!this.favoriteVariantIds.contains(variantId)) this.favoriteVariantIds.add(variantId);
		} else {
			this.favoriteVariantIds.remove(variantId);
		}
	}

	public void setFavoriteEdit(String editId, boolean favorite) {
		if (this.favoriteEditIds == null) this.favoriteEditIds = new ArrayList<>();
		if (favorite) {
			if (!this.favoriteEditIds.contains(editId)) this.favoriteEditIds.add(editId);
		} else {
			this.favoriteEditIds.remove(editId);
		}
	}
}
