package com.monovai.domain.business.edit.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.edit.dto.request.EditImageRequest;
import com.monovai.domain.business.edit.dto.response.EditCreatedResponse;
import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.enums.BaseSourceKind;
import com.monovai.domain.business.edit.entity.enums.EditMode;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.edit.repository.ImageEditRepository;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
import com.monovai.domain.business.imagejob.entity.enums.Angle;
import com.monovai.domain.business.imagejob.entity.enums.Lighting;
import com.monovai.domain.business.imagejob.entity.enums.Ratio;
import com.monovai.domain.business.imagejob.repository.ImageJobRepository;
import com.monovai.domain.business.recommendation.entity.value.GlobalLock;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.common.util.SlugGenerator;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.ForbiddenException;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.worker.business.event.EditCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EditService {

	private static final String SLUG_PREFIX = "bizedit";

	private final ImageEditRepository editRepository;
	private final ImageJobRepository jobRepository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public EditCreatedResponse create(Long userId, EditImageRequest request) {
		// 1. mode 변환 + params 검증
		EditMode mode = EditMode.from(request.mode());
		EditParams params = request.params();
		validateParams(mode, params);

		// 2. 부모 ImageJob 조회 + 권한
		ImageJob job = jobRepository.findByJobSlug(request.jobId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.IMAGE_JOB_NOT_FOUND));
		if (!job.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}

		// 3. baseId 해석 — variant 또는 edit
		BaseSnapshot snapshot = resolveBase(job, request.baseId());

		// 4. User 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		// 5. ImageEdit 생성 (PENDING)
		ImageEdit edit = ImageEdit.create(
			slugGenerator.generate(SLUG_PREFIX), user, job,
			request.baseId(), snapshot.kind(), snapshot.id(),
			snapshot.s3Key(), snapshot.ratio(), snapshot.globalLock(), snapshot.recommendationTitle(),
			mode, params
		);
		edit = editRepository.save(edit);

		// 6. 비동기 워커 트리거
		eventPublisher.publishEvent(new EditCreatedEvent(edit.getId()));

		log.info("[EditService] created editId={} jobId={} baseRef={} mode={}",
			edit.getEditSlug(), job.getJobSlug(), request.baseId(), mode);
		return EditCreatedResponse.of(edit.getEditSlug());
	}

	/**
	 * baseId("V1" 또는 "bizedit_...") 를 해석해서 base 스냅샷 데이터 반환.
	 */
	private BaseSnapshot resolveBase(ImageJob job, String baseRef) {
		if (baseRef == null || baseRef.isBlank()) {
			throw new BadRequestException(ErrorCode.MISSING_PARAMETER);
		}

		// 1) "V<숫자>" — variant
		if (baseRef.matches("V\\d+")) {
			int seq = Integer.parseInt(baseRef.substring(1));
			ImageJobVariant variant = job.getVariants().stream()
				.filter(v -> v.getVariantSeq() == seq)
				.findFirst()
				.orElseThrow(() -> new NotFoundException(ErrorCode.BASE_NOT_FOUND));
			if (variant.getResultS3Key() == null) {
				throw new BadRequestException(ErrorCode.BASE_NOT_READY);
			}
			return new BaseSnapshot(
				BaseSourceKind.VARIANT, variant.getId(),
				variant.getResultS3Key(), job.getRatio(), variant.getGlobalLock(),
				variant.getRecommendationTitle()
			);
		}

		// 2) bizedit_... — edit (체이닝)
		ImageEdit baseEdit = editRepository.findByEditSlug(baseRef)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BASE_NOT_FOUND));
		if (!baseEdit.getRootJob().getId().equals(job.getId())) {
			throw new BadRequestException(ErrorCode.BASE_NOT_FOUND);
		}
		if (baseEdit.getResultS3Key() == null) {
			throw new BadRequestException(ErrorCode.BASE_NOT_READY);
		}
		return new BaseSnapshot(
			BaseSourceKind.EDIT, baseEdit.getId(),
			baseEdit.getResultS3Key(), baseEdit.getBaseRatio(), baseEdit.getBaseGlobalLock(),
			baseEdit.getBaseRecommendationTitle()
		);
	}

	private void validateParams(EditMode mode, EditParams p) {
		if (p == null) {
			throw new BadRequestException(ErrorCode.INVALID_EDIT_PARAMS);
		}
		switch (mode) {
			case BACKGROUND_CHANGE, OBJECT_ADD -> {
				if (!p.hasDescription() && !p.hasReferenceImage()) {
					throw new BadRequestException(ErrorCode.INVALID_EDIT_PARAMS);
				}
			}
			case PRODUCT_REPLACE -> {
				if (!p.hasReferenceImage()) {
					throw new BadRequestException(ErrorCode.INVALID_EDIT_PARAMS);
				}
			}
			case LIGHTING_CHANGE -> Lighting.from(p.lighting());   // throws if invalid
			case ANGLE_CHANGE -> Angle.from(p.angle());
			case RATIO_CHANGE -> Ratio.from(p.ratio());
		}
	}

	private record BaseSnapshot(
		BaseSourceKind kind,
		Long id,
		String s3Key,
		Ratio ratio,
		GlobalLock globalLock,
		String recommendationTitle
	) {
	}
}