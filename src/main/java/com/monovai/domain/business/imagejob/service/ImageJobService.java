package com.monovai.domain.business.imagejob.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.repository.ImageEditRepository;
import com.monovai.domain.business.imagejob.dto.request.GenerateImageRequest;
import com.monovai.domain.business.imagejob.dto.response.ImageJobCreatedResponse;
import com.monovai.domain.business.imagejob.dto.response.ImageJobResponse;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
import com.monovai.domain.business.imagejob.entity.enums.Angle;
import com.monovai.domain.business.imagejob.entity.enums.Lighting;
import com.monovai.domain.business.imagejob.entity.enums.Ratio;
import com.monovai.domain.business.imagejob.repository.ImageJobRepository;
import com.monovai.domain.business.recommendation.entity.RecommendationRequest;
import com.monovai.domain.business.recommendation.entity.enums.RecommendationStatus;
import com.monovai.domain.business.recommendation.entity.value.RecommendationItem;
import com.monovai.domain.business.recommendation.repository.RecommendationRequestRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.common.util.SlugGenerator;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.ForbiddenException;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.infrastructure.s3.service.S3Service;
import com.monovai.worker.business.event.ImageJobCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ImageJobService {

	private static final String SLUG_PREFIX = "bizimg";
	private static final Duration RESULT_URL_TTL = Duration.ofDays(7);

	private final ImageJobRepository jobRepository;
	private final ImageEditRepository editRepository;
	private final RecommendationRequestRepository requestRepository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final ApplicationEventPublisher eventPublisher;
	private final S3Service s3Service;

	@Transactional
	public ImageJobCreatedResponse create(Long userId, GenerateImageRequest request) {
		// 1. enum 변환 + 유효성 검증
		Angle angle = Angle.from(request.angle());
		Lighting lighting = Lighting.from(request.lighting());
		Ratio ratio = Ratio.from(request.ratio());

		// 2. RecommendationRequest 조회 + 권한 + 상태 검증
		RecommendationRequest req = requestRepository.findByRequestSlug(request.requestId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.RECOMMENDATION_NOT_FOUND));

		if (!req.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}
		if (req.getStatus() != RecommendationStatus.COMPLETED) {
			throw new BadRequestException(ErrorCode.RECOMMENDATION_NOT_READY);
		}

		// 3. recommendationIds 검증 — request.recommendations 안에 실제로 존재해야 함
		List<RecommendationItem> selected = filterSelectedRecommendations(req, request.recommendationIds());

		// 4. User 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		// 5. ImageJob 생성 (PENDING) + variants 스냅샷
		ImageJob job = ImageJob.create(
			slugGenerator.generate(SLUG_PREFIX), user, req, angle, lighting, ratio
		);
		int seq = 1;
		for (RecommendationItem item : selected) {
			ImageJobVariant variant = ImageJobVariant.create(
				job, seq++,
				item.id(), item.title(), item.description(), item.globalLock()
			);
			job.addVariant(variant);
		}
		job = jobRepository.save(job);

		// 6. 비동기 워커 트리거 (트랜잭션 커밋 후에 처리됨)
		eventPublisher.publishEvent(new ImageJobCreatedEvent(job.getId()));

		log.info("[ImageJobService] created jobId={} variants={}", job.getJobSlug(), job.getVariants().size());
		return ImageJobCreatedResponse.of(job.getJobSlug());
	}

	public ImageJobResponse get(Long userId, String jobId) {
		ImageJob job = jobRepository.findWithVariantsByJobSlug(jobId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.IMAGE_JOB_NOT_FOUND));

		if (!job.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}

		List<ImageEdit> edits = editRepository.findAllByRootJob_IdOrderByCreatedAtAsc(job.getId());

		// variant 결과 — 매 호출마다 7일짜리 presigned URL 새로 발급
		Map<Long, String> variantUrls = new HashMap<>();
		for (ImageJobVariant v : job.getVariants()) {
			if (v.getResultS3Key() != null) {
				variantUrls.put(v.getId(),
					s3Service.getPreSignedUrlForDownload(v.getResultS3Key(), RESULT_URL_TTL));
			}
		}

		// edit 결과/base 도 매번 재서명
		List<ImageJobResponse.EditView> editViews = edits.stream()
			.map(e -> {
				String baseUrl = e.getBaseS3Key() != null
					? s3Service.getPreSignedUrlForDownload(e.getBaseS3Key(), RESULT_URL_TTL) : null;
				String resultUrl = e.getResultS3Key() != null
					? s3Service.getPreSignedUrlForDownload(e.getResultS3Key(), RESULT_URL_TTL) : null;
				return ImageJobResponse.EditView.of(e, job.getJobSlug(), baseUrl, resultUrl);
			})
			.toList();

		return ImageJobResponse.of(job, editViews, variantUrls);
	}

	private List<RecommendationItem> filterSelectedRecommendations(
		RecommendationRequest req, List<String> pickedIds
	) {
		List<RecommendationItem> all = req.getRecommendations();
		if (all == null || all.isEmpty()) {
			throw new BadRequestException(ErrorCode.RECOMMENDATION_NOT_READY);
		}

		Set<String> validIds = new HashSet<>();
		for (RecommendationItem item : all) {
			validIds.add(item.id());
		}
		for (String picked : pickedIds) {
			if (!validIds.contains(picked)) {
				throw new NotFoundException(ErrorCode.RECOMMENDATION_ID_NOT_FOUND);
			}
		}

		// 사용자가 보낸 순서대로 V1, V2, ...
		return pickedIds.stream()
			.map(id -> all.stream().filter(r -> id.equals(r.id())).findFirst().orElseThrow())
			.toList();
	}
}