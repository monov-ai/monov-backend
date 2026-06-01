package com.monovai.domain.business.imagejob.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.enums.EditMode;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.edit.repository.ImageEditRepository;
import com.monovai.domain.business.imagejob.dto.request.CreateJobFromUploadRequest;
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
	private static final Duration FETCHABLE_URL_TTL = Duration.ofMinutes(60);

	private final ImageJobRepository jobRepository;
	private final ImageEditRepository editRepository;
	private final RecommendationRequestRepository requestRepository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final ApplicationEventPublisher eventPublisher;
	private final S3Service s3Service;

	@Transactional
	public ImageJobCreatedResponse create(Long userId, GenerateImageRequest request) {
		Angle angle = (request.angle() == null || request.angle().isBlank()) ? null : Angle.from(request.angle());
		Lighting lighting = Lighting.from(request.lighting());
		Ratio ratio = Ratio.from(request.ratio());

		RecommendationRequest req = requestRepository.findByRequestSlug(request.requestId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.RECOMMENDATION_NOT_FOUND));

		if (!req.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}
		if (req.getStatus() != RecommendationStatus.COMPLETED) {
			throw new BadRequestException(ErrorCode.RECOMMENDATION_NOT_READY);
		}

		List<String> pickedIds = request.normalizedRecommendationIds();
		if (pickedIds.isEmpty()) {
			throw new BadRequestException(ErrorCode.MISSING_PARAMETER);
		}

		List<RecommendationItem> selected = filterSelectedRecommendations(req, pickedIds);

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		ImageJob job = ImageJob.create(
			slugGenerator.generate(SLUG_PREFIX), user, req, angle, lighting, ratio,
			request.transparentBackground()
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

		eventPublisher.publishEvent(new ImageJobCreatedEvent(job.getId()));

		log.info("[ImageJobService] created jobId={} variants={}", job.getJobSlug(), job.getVariants().size());
		return ImageJobCreatedResponse.of(job.getJobSlug());
	}

	/**
	 * §15: 사용자가 업로드한 이미지 1장을 V1=완료 상태로 등록. Nanobanana 호출 없음.
	 * imagePath 의 prefix 가 호출 사용자 ID 와 일치하는지 검증 (SSRF/도용 방지).
	 */
	@Transactional
	public ImageJobCreatedResponse createFromUpload(Long userId, CreateJobFromUploadRequest request) {
		Ratio ratio = Ratio.from(request.ratio());

		validateOwnedS3Key(request.imagePath(), userId);

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		ImageJob job = ImageJob.createFromUpload(
			slugGenerator.generate(SLUG_PREFIX), user, ratio,
			request.imageUrl(), request.imagePath()
		);
		ImageJobVariant variant = ImageJobVariant.createFromUpload(job, request.title(), request.imagePath());
		job.addVariant(variant);

		job = jobRepository.save(job);

		log.info("[ImageJobService] user_upload jobId={} ratio={} title={}",
			job.getJobSlug(), ratio.getValue(), request.title());
		return ImageJobCreatedResponse.of(job.getJobSlug());
	}

	/**
	 * S3 key 가 사용자 본인 prefix 에 속하는지 검사. 우리 S3 업로드 키 컨벤션은
	 * `users/{userId}/...` 또는 `business_uploads/{userId}/...` 같은 prefix 패턴.
	 * 둘 다 허용하고, 어느 쪽도 아니면 reject.
	 */
	private void validateOwnedS3Key(String key, Long userId) {
		if (key == null || key.isBlank()) {
			throw new BadRequestException(ErrorCode.MISSING_PARAMETER);
		}
		String uid = String.valueOf(userId);
		boolean ok = key.contains("/" + uid + "/") || key.startsWith(uid + "/");
		if (!ok) {
			log.warn("[ImageJobService] foreign S3 key 요청 거부 userId={} key={}", userId, key);
			throw new ForbiddenException(ErrorCode.S3_KEY_FORBIDDEN);
		}
	}

	public ImageJobResponse get(Long userId, String jobId) {
		ImageJob job = jobRepository.findWithVariantsByJobSlug(jobId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.IMAGE_JOB_NOT_FOUND));

		if (!job.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}

		List<ImageEdit> edits = editRepository.findAllByRootJob_IdOrderByCreatedAtAsc(job.getId());

		Map<Long, String> variantUrls = new HashMap<>();
		for (ImageJobVariant v : job.getVariants()) {
			if (v.getResultS3Key() != null) {
				variantUrls.put(v.getId(),
					s3Service.getPreSignedUrlForDownload(v.getResultS3Key(), RESULT_URL_TTL));
			}
		}

		String fetchableImageUrl = signKeyIfPresent(job.getProductImagePath());
		List<String> fetchableProductImageUrls = signList(job.getProductImagePaths());
		List<String> fetchableReferenceImageUrls = signList(job.getReferenceImagePaths());

		List<ImageJobResponse.EditView> editViews = new ArrayList<>();
		for (ImageEdit e : edits) {
			String baseUrl = e.getBaseS3Key() != null
				? s3Service.getPreSignedUrlForDownload(e.getBaseS3Key(), RESULT_URL_TTL) : null;
			String resultUrl = e.getResultS3Key() != null
				? s3Service.getPreSignedUrlForDownload(e.getResultS3Key(), RESULT_URL_TTL) : null;
			String baseFetchable = e.getBaseS3Key() != null
				? s3Service.getPreSignedUrlForDownload(e.getBaseS3Key(), FETCHABLE_URL_TTL) : null;
			String singleRefFetchable = null;
			List<String> refFetchableList = new ArrayList<>();
			EditParams params = e.getParams();
			if (params != null) {
				if (params.referenceImagePath() != null && !params.referenceImagePath().isBlank()) {
					singleRefFetchable = s3Service.getPreSignedUrlForDownload(
						params.referenceImagePath(), FETCHABLE_URL_TTL);
				}
				if (params.referenceImagePaths() != null) {
					for (String k : params.referenceImagePaths()) {
						if (k != null && !k.isBlank()) {
							refFetchableList.add(s3Service.getPreSignedUrlForDownload(k, FETCHABLE_URL_TTL));
						}
					}
				}
			}
			editViews.add(ImageJobResponse.EditView.of(
				e, job.getJobSlug(), job.getUser().getId(),
				job.getRequest() == null ? null : job.getRequest().getRequestSlug(),
				baseUrl, resultUrl, baseFetchable, singleRefFetchable, refFetchableList
			));

			// inpaint 처럼 mode 가 INPAINT 면 status 보존 흐름은 worker 가 채움
			if (e.getMode() == EditMode.INPAINT) {
				log.debug("[ImageJob/get] INPAINT edit included {}", e.getEditSlug());
			}
		}

		return ImageJobResponse.of(job, editViews, variantUrls,
			fetchableImageUrl, fetchableProductImageUrls, fetchableReferenceImageUrls);
	}

	private String signKeyIfPresent(String key) {
		if (key == null || key.isBlank()) return null;
		return s3Service.getPreSignedUrlForDownload(key, FETCHABLE_URL_TTL);
	}

	private List<String> signList(List<String> keys) {
		List<String> out = new ArrayList<>();
		if (keys == null) return out;
		for (String k : keys) {
			if (k != null && !k.isBlank()) {
				out.add(s3Service.getPreSignedUrlForDownload(k, FETCHABLE_URL_TTL));
			}
		}
		return out;
	}

	private List<RecommendationItem> filterSelectedRecommendations(
		RecommendationRequest req, List<String> pickedIds
	) {
		List<RecommendationItem> all = req.getRecommendations();
		if (all == null || all.isEmpty()) {
			throw new BadRequestException(ErrorCode.RECOMMENDATION_NOT_READY);
		}

		Set<String> validIds = new HashSet<>();
		for (RecommendationItem item : all) validIds.add(item.id());
		for (String picked : pickedIds) {
			if (!validIds.contains(picked)) {
				throw new NotFoundException(ErrorCode.RECOMMENDATION_ID_NOT_FOUND);
			}
		}

		return pickedIds.stream()
			.map(id -> all.stream().filter(r -> id.equals(r.id())).findFirst().orElseThrow())
			.toList();
	}
}
