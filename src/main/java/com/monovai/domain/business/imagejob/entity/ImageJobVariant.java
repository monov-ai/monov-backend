package com.monovai.domain.business.imagejob.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.business.imagejob.entity.enums.VariantStatus;
import com.monovai.domain.business.recommendation.entity.value.GlobalLock;
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

/**
 * ImageJob 의 자식 엔티티. 사용자가 선택한 추천 1건당 variant 1개.
 * 추천 데이터 (id/title/description/globalLock) 를 스냅샷으로 박제.
 */
@Entity
@Table(name = "image_job_variants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageJobVariant extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id", nullable = false)
	private ImageJob job;

	/** 같은 job 안에서 선택 순서 (1, 2, 3). 외부 노출은 "V"+seq. */
	@Column(nullable = false)
	private Integer variantSeq;

	// 추천 스냅샷
	@Column(nullable = false, length = 100)
	private String recommendationId;

	@Column(nullable = false, length = 200)
	private String recommendationTitle;

	@Column(columnDefinition = "TEXT")
	private String recommendationDescription;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private GlobalLock globalLock;

	// 워커가 채우는 필드
	@Column(columnDefinition = "TEXT")
	private String imagePrompt;

	@Column(length = 100)
	private String nanobananaTaskId;

	/** S3 객체 키. URL 은 응답 시점에 presigned URL 로 재서명. */
	@Column(name = "result_s3_key", columnDefinition = "TEXT")
	private String resultS3Key;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VariantStatus status;

	@Builder(access = AccessLevel.PRIVATE)
	private ImageJobVariant(
		ImageJob job, Integer variantSeq,
		String recommendationId, String recommendationTitle, String recommendationDescription,
		GlobalLock globalLock, VariantStatus status
	) {
		this.job = job;
		this.variantSeq = variantSeq;
		this.recommendationId = recommendationId;
		this.recommendationTitle = recommendationTitle;
		this.recommendationDescription = recommendationDescription;
		this.globalLock = globalLock;
		this.status = status;
	}

	public static ImageJobVariant create(
		ImageJob job, Integer variantSeq,
		String recommendationId, String recommendationTitle, String recommendationDescription,
		GlobalLock globalLock
	) {
		return ImageJobVariant.builder()
			.job(job)
			.variantSeq(variantSeq)
			.recommendationId(recommendationId)
			.recommendationTitle(recommendationTitle)
			.recommendationDescription(recommendationDescription)
			.globalLock(globalLock)
			.status(VariantStatus.PENDING)
			.build();
	}

	public String getVariantId() {
		return "V" + variantSeq;
	}

	public void markRunning(String imagePrompt) {
		this.status = VariantStatus.RUNNING;
		this.imagePrompt = imagePrompt;
	}

	public void markSucceeded(String nanobananaTaskId, String resultS3Key) {
		this.status = VariantStatus.COMPLETED;
		this.nanobananaTaskId = nanobananaTaskId;
		this.resultS3Key = resultS3Key;
	}

	public void markFailed(String errorMessage) {
		this.status = VariantStatus.FAILED;
		this.errorMessage = errorMessage;
	}
}