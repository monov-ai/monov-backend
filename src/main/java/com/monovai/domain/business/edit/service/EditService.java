package com.monovai.domain.business.edit.service;

import java.util.List;

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
	private static final int MAX_REFERENCE_IMAGES = 4;

	private final ImageEditRepository editRepository;
	private final ImageJobRepository jobRepository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public EditCreatedResponse create(Long userId, EditImageRequest request) {
		EditMode mode = EditMode.from(request.mode());
		if (mode == EditMode.INPAINT) {
			// inpaint 는 별도 endpoint (/api/v1/business/inpaint) 만 허용 — 여기서 거부.
			throw new BadRequestException(ErrorCode.INVALID_EDIT_MODE);
		}

		EditParams params = request.params() != null ? request.params() : EditParams.empty();
		validateParams(mode, params);

		ImageJob job = jobRepository.findByJobSlug(request.jobId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.IMAGE_JOB_NOT_FOUND));
		if (!job.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		ImageEdit edit;
		if (mode == EditMode.TEXT_CREATE) {
			// base 없음
			edit = ImageEdit.create(
				slugGenerator.generate(SLUG_PREFIX), user, job,
				null, null, null,
				null, job.getRatio(), null, null,
				mode, params
			);
		} else {
			String baseRef = request.resolvedBaseId();
			if (baseRef == null || baseRef.isBlank()) {
				throw new BadRequestException(ErrorCode.MISSING_PARAMETER);
			}
			BaseSnapshot snapshot = resolveBase(job, baseRef);
			edit = ImageEdit.create(
				slugGenerator.generate(SLUG_PREFIX), user, job,
				baseRef, snapshot.kind(), snapshot.id(),
				snapshot.s3Key(), snapshot.ratio(), snapshot.globalLock(), snapshot.recommendationTitle(),
				mode, params
			);
			if (mode == EditMode.RATIO_CHANGE && params.ratio() != null) {
				edit.setAppliedRatio(Ratio.from(params.ratio()));
			}
		}
		edit = editRepository.save(edit);

		eventPublisher.publishEvent(new EditCreatedEvent(edit.getId()));

		log.info("[EditService] created editId={} jobId={} mode={}", edit.getEditSlug(), job.getJobSlug(), mode);
		return EditCreatedResponse.of(edit.getEditSlug());
	}

	private BaseSnapshot resolveBase(ImageJob job, String baseRef) {
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

		ImageEdit baseEdit = editRepository.findByEditSlug(baseRef)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BASE_NOT_FOUND));
		if (!baseEdit.getRootJob().getId().equals(job.getId())) {
			throw new BadRequestException(ErrorCode.BASE_NOT_FOUND);
		}
		if (baseEdit.getResultS3Key() == null) {
			throw new BadRequestException(ErrorCode.BASE_NOT_READY);
		}
		Ratio chainRatio = baseEdit.getAppliedRatio() != null
			? baseEdit.getAppliedRatio() : baseEdit.getBaseRatio();
		return new BaseSnapshot(
			BaseSourceKind.EDIT, baseEdit.getId(),
			baseEdit.getResultS3Key(), chainRatio, baseEdit.getBaseGlobalLock(),
			baseEdit.getBaseRecommendationTitle()
		);
	}

	private void validateParams(EditMode mode, EditParams p) {
		List<String> refs = p.collectReferenceUrls();
		if (refs.size() > MAX_REFERENCE_IMAGES) {
			throw new BadRequestException(ErrorCode.TOO_MANY_IMAGES);
		}
		switch (mode) {
			case BACKGROUND_CHANGE, OBJECT_ADD -> {
				if (!p.hasDescription() && !p.hasReferenceImage()) {
					throw new BadRequestException(ErrorCode.INVALID_EDIT_PARAMS);
				}
			}
			case PRODUCT_REPLACE -> {
				if (refs.isEmpty()) throw new BadRequestException(ErrorCode.INVALID_EDIT_PARAMS);
			}
			case LIGHTING_CHANGE -> Lighting.from(p.lighting());
			case ANGLE_CHANGE -> validateAngleParams(p);
			case RATIO_CHANGE -> Ratio.from(p.ratio());
			case TEXT_CREATE -> {
				if (!p.hasDescription()) throw new BadRequestException(ErrorCode.INVALID_TEXT_CREATE_PROMPT);
				int len = p.description() == null ? 0 : p.description().length();
				if (len < 1 || len > 500) throw new BadRequestException(ErrorCode.INVALID_TEXT_CREATE_PROMPT);
			}
			case INPAINT -> {
				// reachable only via dedicated endpoint
			}
		}
	}

	private void validateAngleParams(EditParams p) {
		if (p.hasAngleCoordinates()) {
			double rot = p.rotation() != null ? p.rotation() : 0.0;
			double tilt = p.tilt() != null ? p.tilt() : 0.0;
			if (rot == 0.0 && tilt == 0.0) throw new BadRequestException(ErrorCode.INVALID_ROTATION_TILT);
			if (rot < -180.0 || rot > 180.0) throw new BadRequestException(ErrorCode.INVALID_ROTATION_TILT);
			if (tilt < -90.0 || tilt > 90.0) throw new BadRequestException(ErrorCode.INVALID_ROTATION_TILT);
			return;
		}
		// legacy enum fallback
		if (p.angle() == null) throw new BadRequestException(ErrorCode.INVALID_EDIT_PARAMS);
		com.monovai.domain.business.imagejob.entity.enums.Angle.from(p.angle());
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
