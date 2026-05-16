package com.monovai.domain.business.history.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.repository.ImageEditRepository;
import com.monovai.domain.business.history.dto.response.HistoryResponse;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
import com.monovai.domain.business.imagejob.repository.ImageJobRepository;
import com.monovai.domain.business.video.entity.VideoTemplate;
import com.monovai.domain.business.video.repository.VideoTemplateRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

	private static final Duration RESULT_URL_TTL = Duration.ofDays(7);

	private final ImageJobRepository jobRepository;
	private final ImageEditRepository editRepository;
	private final VideoTemplateRepository videoRepository;
	private final S3Service s3Service;

	public HistoryResponse list(Long userId, int limit) {
		if (limit < 1 || limit > 200) {
			throw new BadRequestException(ErrorCode.HISTORY_LIMIT_INVALID);
		}

		List<ImageJob> jobs = jobRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
		List<Long> jobIds = jobs.stream().map(ImageJob::getId).toList();
		Map<Long, List<ImageEdit>> editsByJob = new HashMap<>();
		if (!jobIds.isEmpty()) {
			List<ImageEdit> edits = editRepository.findAllByRootJob_IdInOrderByCreatedAtAsc(jobIds);
			for (ImageEdit e : edits) {
				editsByJob.computeIfAbsent(e.getRootJob().getId(), k -> new ArrayList<>()).add(e);
			}
		}

		List<HistoryResponse.HistoryItem> items = new ArrayList<>();
		for (ImageJob j : jobs) {
			items.add(toItem(j, editsByJob.getOrDefault(j.getId(), List.of())));
		}

		List<VideoTemplate> videos = videoRepository.findAllByUser_IdAndSourceOrderByCreatedAtDesc(userId, "business");
		for (VideoTemplate v : videos) {
			items.add(toItem(v));
		}

		items.sort(Comparator.comparing(
			HistoryResponse.HistoryItem::createdAt,
			Comparator.nullsLast(Comparator.reverseOrder())
		));
		if (items.size() > limit) items = items.subList(0, limit);

		return HistoryResponse.of(items);
	}

	private HistoryResponse.HistoryItem toItem(ImageJob j, List<ImageEdit> edits) {
		List<HistoryResponse.VariantView> variants = new ArrayList<>();
		for (ImageJobVariant v : j.getVariants()) {
			String url = v.getResultS3Key() == null ? null
				: s3Service.getPreSignedUrlForDownload(v.getResultS3Key(), RESULT_URL_TTL);
			variants.add(new HistoryResponse.VariantView(
				v.getVariantId(), v.getRecommendationId(), v.getRecommendationTitle(),
				v.getRecommendationDescription(), v.getGlobalLock(),
				url, v.getErrorMessage()
			));
		}
		List<HistoryResponse.EditView> editViews = new ArrayList<>();
		for (ImageEdit e : edits) {
			String url = e.getResultS3Key() == null ? null
				: s3Service.getPreSignedUrlForDownload(e.getResultS3Key(), RESULT_URL_TTL);
			editViews.add(new HistoryResponse.EditView(
				e.getEditSlug(), e.getMode().getValue(),
				e.getBaseGlobalLock(), e.getBaseRecommendationTitle(),
				e.getBaseRatio() != null ? e.getBaseRatio().getValue() : null,
				e.getAppliedRatio() != null ? e.getAppliedRatio().getValue()
					: (e.getBaseRatio() != null ? e.getBaseRatio().getValue() : null),
				url, e.getErrorMessage(), e.getStatus().getValue(),
				e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null
			));
		}
		return new HistoryResponse.HistoryItem(
			j.getJobSlug(),
			"imagejob",
			j.getUser().getId(),
			j.getRequest() != null ? j.getRequest().getRequestSlug() : null,
			null,
			null,
			null,
			j.getStyle().getValue(),
			j.getStyle().getLabel(),
			j.getDescription(),
			j.getProductImageUrl(),
			j.getAngle() == null ? null : j.getAngle().getValue(),
			j.getLighting().getValue(),
			j.getRatio().getValue(),
			j.getStatus().getValue(),
			j.getCreatedAt() != null ? j.getCreatedAt().toInstant() : null,
			j.getUpdatedAt() != null ? j.getUpdatedAt().toInstant() : null,
			variants, editViews,
			j.getFavoriteVariantIds() != null ? j.getFavoriteVariantIds() : List.of(),
			j.getFavoriteEditIds() != null ? j.getFavoriteEditIds() : List.of()
		);
	}

	private HistoryResponse.HistoryItem toItem(VideoTemplate v) {
		HistoryResponse.VariantView single = new HistoryResponse.VariantView(
			v.getVideoSlug(),
			null,
			v.getTitle(),
			v.getUserPrompt(),
			null,
			v.getResultMediaUrl(),
			v.getErrorMessage()
		);
		return new HistoryResponse.HistoryItem(
			v.getVideoSlug(),
			"template",
			v.getUser().getId(),
			null,
			v.getTemplateId(),
			v.getMediaType().getValue(),
			v.getResultMediaUrl(),
			"template",
			v.getTitle() != null ? v.getTitle() : "템플릿",
			v.getUserPrompt(),
			v.getSourceImageUrl(),
			null,
			null,
			v.getRatio(),
			v.getStatus().getValue(),
			v.getCreatedAt() != null ? v.getCreatedAt().toInstant() : null,
			v.getUpdatedAt() != null ? v.getUpdatedAt().toInstant() : null,
			List.of(single),
			List.of(),
			List.of(),
			List.of()
		);
	}
}
