package com.monovai.domain.business.video.entity;

import com.monovai.domain.business.video.entity.enums.MediaType;
import com.monovai.domain.business.video.entity.enums.VideoStatus;
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

/**
 * 비즈니스 / 스튜디오 흐름에서 만든 영상·이미지 템플릿 결과물. spec 의 `monovTemplates(Img)` 1:1.
 */
@Entity
@Table(name = "video_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoTemplate extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, length = 50)
	private String videoSlug;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(length = 50)
	private String source;          // "business" | "studio"

	@Column(length = 30)
	private String model;           // "cling" | "sora" | "nanobanana"

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MediaType mediaType;

	@Column(length = 200)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String userPrompt;

	@Column(columnDefinition = "TEXT")
	private String templatePrompt;

	@Column(length = 50)
	private String templateId;

	@Column(length = 20)
	private String ratio;

	// 원본 이미지 (변환 입력)
	@Column(columnDefinition = "TEXT")
	private String sourceImageUrl;

	@Column(columnDefinition = "TEXT")
	private String sourceImagePath;

	// 트래킹 — 원본 ImageJob / variant / edit
	@Column(length = 50)
	private String sourceJobId;

	@Column(length = 50)
	private String sourceItemId;

	@Column(length = 20)
	private String sourceKind;       // "variant" | "edit"

	@Column(length = 100)
	private String kieTaskId;

	@Column(columnDefinition = "TEXT")
	private String resultMediaUrl;

	@Column(columnDefinition = "TEXT")
	private String resultMediaPath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VideoStatus status;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Column(nullable = false)
	private boolean favorite;

	@Builder(access = AccessLevel.PRIVATE)
	private VideoTemplate(
		String videoSlug, User user,
		String source, String model, MediaType mediaType,
		String title, String userPrompt, String templatePrompt, String templateId, String ratio,
		String sourceImageUrl, String sourceImagePath,
		String sourceJobId, String sourceItemId, String sourceKind,
		VideoStatus status
	) {
		this.videoSlug = videoSlug;
		this.user = user;
		this.source = source;
		this.model = model;
		this.mediaType = mediaType;
		this.title = title;
		this.userPrompt = userPrompt;
		this.templatePrompt = templatePrompt;
		this.templateId = templateId;
		this.ratio = ratio;
		this.sourceImageUrl = sourceImageUrl;
		this.sourceImagePath = sourceImagePath;
		this.sourceJobId = sourceJobId;
		this.sourceItemId = sourceItemId;
		this.sourceKind = sourceKind;
		this.status = status;
		this.favorite = false;
	}

	public static VideoTemplate create(
		String videoSlug, User user,
		String source, String model, MediaType mediaType,
		String title, String userPrompt, String templatePrompt, String templateId, String ratio,
		String sourceImageUrl, String sourceImagePath,
		String sourceJobId, String sourceItemId, String sourceKind
	) {
		return VideoTemplate.builder()
			.videoSlug(videoSlug).user(user)
			.source(source).model(model).mediaType(mediaType)
			.title(title).userPrompt(userPrompt).templatePrompt(templatePrompt).templateId(templateId)
			.ratio(ratio)
			.sourceImageUrl(sourceImageUrl).sourceImagePath(sourceImagePath)
			.sourceJobId(sourceJobId).sourceItemId(sourceItemId).sourceKind(sourceKind)
			.status(VideoStatus.REQUESTED)
			.build();
	}

	public void markRunning(String kieTaskId) {
		this.status = VideoStatus.RUNNING;
		this.kieTaskId = kieTaskId;
	}

	public void markCompleted(String url, String path) {
		this.status = VideoStatus.COMPLETED;
		this.resultMediaUrl = url;
		this.resultMediaPath = path;
	}

	public void markFailed(String message) {
		this.status = VideoStatus.FAILED;
		this.errorMessage = message;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}
}
