package com.monovai.domain.business.edit.service;

import java.util.Base64;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.edit.dto.request.InpaintRequest;
import com.monovai.domain.business.edit.dto.response.EditCreatedResponse;
import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.enums.BaseSourceKind;
import com.monovai.domain.business.edit.entity.enums.EditMode;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.edit.repository.ImageEditRepository;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
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
import com.monovai.infrastructure.s3.service.S3Service;
import com.monovai.worker.business.event.EditCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InpaintService {

	private static final String SLUG_PREFIX = "bizedit";
	private static final int MAX_MASK_BYTES = 4 * 1024 * 1024;

	private final ImageEditRepository editRepository;
	private final ImageJobRepository jobRepository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final ApplicationEventPublisher eventPublisher;
	private final S3Service s3Service;

	@Transactional
	public EditCreatedResponse create(Long userId, InpaintRequest request) {
		String baseRef = request.resolvedBaseId();
		if (baseRef == null || baseRef.isBlank()) {
			throw new BadRequestException(ErrorCode.MISSING_PARAMETER);
		}

		ImageJob job = jobRepository.findByJobSlug(request.jobId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.IMAGE_JOB_NOT_FOUND));
		if (!job.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}

		BaseSnapshot snapshot = resolveBase(job, baseRef);

		byte[] mask = decodeMask(request.maskBase64());
		if (mask.length > MAX_MASK_BYTES) {
			throw new BadRequestException(ErrorCode.MASK_TOO_LARGE);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		ImageEdit edit = ImageEdit.create(
			slugGenerator.generate(SLUG_PREFIX), user, job,
			baseRef, snapshot.kind(), snapshot.id(),
			snapshot.s3Key(), snapshot.ratio(), snapshot.globalLock(), snapshot.recommendationTitle(),
			EditMode.INPAINT,
			new EditParams(
				null, null, null, null, null,
				null, null, null, null, null,
				request.prompt(), null, null,
				request.maskWidth(), request.maskHeight(),
				request.size() == null ? "auto" : request.size()
			)
		);
		edit = editRepository.save(edit);

		String maskKey = "business_results/" + job.getJobSlug() + "/inpaint/" + edit.getEditSlug() + "_mask.png";
		s3Service.uploadBytes(maskKey, mask, "image/png");

		EditParams oldParams = edit.getParams();
		edit.updateParams(new EditParams(
			null, null, null, null, null, null,
			null, null, null, null,
			oldParams.prompt(), null, maskKey,
			oldParams.maskWidth(), oldParams.maskHeight(),
			oldParams.size()
		));

		eventPublisher.publishEvent(new EditCreatedEvent(edit.getId()));

		log.info("[InpaintService] created editId={} jobId={} maskBytes={}",
			edit.getEditSlug(), job.getJobSlug(), mask.length);
		return EditCreatedResponse.of(edit.getEditSlug());
	}

	private byte[] decodeMask(String base64) {
		String b = base64;
		int comma = b.indexOf(',');
		if (b.startsWith("data:") && comma > 0) {
			b = b.substring(comma + 1);
		}
		try {
			return Base64.getDecoder().decode(b);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(ErrorCode.INVALID_MASK);
		}
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
