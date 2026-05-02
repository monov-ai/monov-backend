package com.monovai.domain.business.edit.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.business.edit.entity.enums.BaseSourceKind;
import com.monovai.domain.business.edit.entity.enums.EditMode;
import com.monovai.domain.business.edit.entity.enums.EditStatus;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.enums.Ratio;
import com.monovai.domain.business.recommendation.entity.value.GlobalLock;
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
@Table(name = "image_edits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageEdit extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, length = 50)
	private String editSlug;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** 체이닝의 root — ImageJob (조회 시 같은 root 의 모든 edits 를 한 번에 끌어옴) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "root_job_id", nullable = false)
	private ImageJob rootJob;

	// ---------- base 정보 (스냅샷) ----------

	/** "V1" (variant) 또는 "bizedit_..." (edit) — 사용자가 보낸 원본 ref */
	@Column(nullable = false, length = 50)
	private String baseRef;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BaseSourceKind baseSourceKind;

	/** ImageJobVariant.id 또는 ImageEdit.id (kind 에 따라). polymorphic 이라 plain Long. */
	@Column(nullable = false)
	private Long baseId;

	@Column(name = "base_s3_key", columnDefinition = "TEXT")
	private String baseS3Key;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Ratio baseRatio;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private GlobalLock baseGlobalLock;

	@Column(length = 200)
	private String baseRecommendationTitle;

	// ---------- 수정 입력 ----------

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EditMode mode;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private EditParams params;

	// ---------- 워커가 채우는 필드 ----------

	@Column(columnDefinition = "TEXT")
	private String imagePrompt;

	@Column(length = 100)
	private String nanobananaTaskId;

	@Column(name = "result_s3_key", columnDefinition = "TEXT")
	private String resultS3Key;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EditStatus status;

	@Builder(access = AccessLevel.PRIVATE)
	private ImageEdit(
		String editSlug, User user, ImageJob rootJob,
		String baseRef, BaseSourceKind baseSourceKind, Long baseId,
		String baseS3Key, Ratio baseRatio, GlobalLock baseGlobalLock, String baseRecommendationTitle,
		EditMode mode, EditParams params,
		EditStatus status
	) {
		this.editSlug = editSlug;
		this.user = user;
		this.rootJob = rootJob;
		this.baseRef = baseRef;
		this.baseSourceKind = baseSourceKind;
		this.baseId = baseId;
		this.baseS3Key = baseS3Key;
		this.baseRatio = baseRatio;
		this.baseGlobalLock = baseGlobalLock;
		this.baseRecommendationTitle = baseRecommendationTitle;
		this.mode = mode;
		this.params = params;
		this.status = status;
	}

	public static ImageEdit create(
		String editSlug, User user, ImageJob rootJob,
		String baseRef, BaseSourceKind baseSourceKind, Long baseId,
		String baseS3Key, Ratio baseRatio, GlobalLock baseGlobalLock, String baseRecommendationTitle,
		EditMode mode, EditParams params
	) {
		return ImageEdit.builder()
			.editSlug(editSlug)
			.user(user)
			.rootJob(rootJob)
			.baseRef(baseRef)
			.baseSourceKind(baseSourceKind)
			.baseId(baseId)
			.baseS3Key(baseS3Key)
			.baseRatio(baseRatio)
			.baseGlobalLock(baseGlobalLock)
			.baseRecommendationTitle(baseRecommendationTitle)
			.mode(mode)
			.params(params)
			.status(EditStatus.PENDING)
			.build();
	}

	public void markRunning(String imagePrompt) {
		this.status = EditStatus.RUNNING;
		this.imagePrompt = imagePrompt;
	}

	public void markSucceeded(String nanobananaTaskId, String resultS3Key) {
		this.status = EditStatus.COMPLETED;
		this.nanobananaTaskId = nanobananaTaskId;
		this.resultS3Key = resultS3Key;
	}

	public void markFailed(String errorMessage) {
		this.status = EditStatus.FAILED;
		this.errorMessage = errorMessage;
	}
}