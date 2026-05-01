package com.monovai.domain.business.edit.entity;

import com.monovai.domain.business.edit.entity.enums.EditStatus;
import com.monovai.domain.business.edit.entity.enums.EditType;
import com.monovai.domain.business.imagejob.entity.ImageJob;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// 체이닝의 root — ImageJob (조회 시 같은 root 의 모든 edits 를 한 번에 끌어옴)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "root_job_id", nullable = false)
	private ImageJob rootJob;

	// 직전 단계 — ImageJob.id 또는 다른 ImageEdit.id 둘 다 가능 (polymorphic).
	// JPA @ManyToOne 으로 깔끔히 모델링 불가하므로 plain Long 유지, EditService 에서 검증.
	@Column(nullable = false)
	private Long baseId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EditType editType;

	@Column(columnDefinition = "TEXT")
	private String prompt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EditStatus status;

	@Column(columnDefinition = "TEXT")
	private String resultImageUrl;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Builder(access = AccessLevel.PRIVATE)
	private ImageEdit(User user, ImageJob rootJob, Long baseId, EditType editType, String prompt, EditStatus status) {
		this.user = user;
		this.rootJob = rootJob;
		this.baseId = baseId;
		this.editType = editType;
		this.prompt = prompt;
		this.status = status;
	}

	public static ImageEdit create(User user, ImageJob rootJob, Long baseId, EditType editType, String prompt) {
		return ImageEdit.builder()
			.user(user)
			.rootJob(rootJob)
			.baseId(baseId)
			.editType(editType)
			.prompt(prompt)
			.status(EditStatus.PENDING)
			.build();
	}

	public void markRunning() {
		this.status = EditStatus.RUNNING;
	}

	public void markSucceeded(String resultImageUrl) {
		this.status = EditStatus.SUCCEEDED;
		this.resultImageUrl = resultImageUrl;
	}

	public void markFailed(String errorMessage) {
		this.status = EditStatus.FAILED;
		this.errorMessage = errorMessage;
	}
}