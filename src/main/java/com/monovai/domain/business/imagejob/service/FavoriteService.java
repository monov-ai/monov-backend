package com.monovai.domain.business.imagejob.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.imagejob.dto.request.FavoriteToggleRequest;
import com.monovai.domain.business.imagejob.dto.response.FavoriteToggleResponse;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.repository.ImageJobRepository;
import com.monovai.domain.business.video.entity.VideoTemplate;
import com.monovai.domain.business.video.entity.enums.MediaType;
import com.monovai.domain.business.video.repository.VideoTemplateRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.ForbiddenException;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

	private final ImageJobRepository jobRepository;
	private final VideoTemplateRepository videoRepository;

	@Transactional
	public FavoriteToggleResponse toggleJobFavorite(Long userId, FavoriteToggleRequest request) {
		ImageJob job = jobRepository.findByJobSlug(request.jobId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.IMAGE_JOB_NOT_FOUND));
		if (!job.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}

		String kind = request.kind().toLowerCase();
		switch (kind) {
			case "variant" -> job.setFavoriteVariant(request.id(), request.favorite());
			case "edit" -> job.setFavoriteEdit(request.id(), request.favorite());
			default -> throw new BadRequestException(ErrorCode.INVALID_FAVORITE_KIND);
		}

		List<String> v = job.getFavoriteVariantIds() != null ? job.getFavoriteVariantIds() : List.of();
		List<String> e = job.getFavoriteEditIds() != null ? job.getFavoriteEditIds() : List.of();
		return FavoriteToggleResponse.of(v, e);
	}

	@Transactional
	public TemplateFavoriteResult toggleTemplateFavorite(Long userId, String itemId, String mediaType, boolean favorite) {
		MediaType.from(mediaType); // 검증
		VideoTemplate v = videoRepository.findByVideoSlug(itemId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_FAVORITE_ITEM_NOT_FOUND));
		if (!v.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}
		v.setFavorite(favorite);
		return new TemplateFavoriteResult(true, v.getVideoSlug(), v.isFavorite());
	}

	public record TemplateFavoriteResult(boolean ok, String itemId, boolean favorite) {
	}
}
