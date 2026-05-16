package com.monovai.domain.business.video.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.video.dto.request.ImageToVideoRequest;
import com.monovai.domain.business.video.dto.response.VideoJobCreatedResponse;
import com.monovai.domain.business.video.dto.response.VideoJobResponse;
import com.monovai.domain.business.video.entity.VideoTemplate;
import com.monovai.domain.business.video.entity.enums.MediaType;
import com.monovai.domain.business.video.repository.VideoTemplateRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.common.util.SlugGenerator;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.ForbiddenException;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.worker.business.event.VideoTemplateCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VideoTemplateService {

	private static final String SLUG_PREFIX = "bvid";

	private final VideoTemplateRepository repository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public VideoJobCreatedResponse createImageToVideo(Long userId, ImageToVideoRequest request) {
		if (request.imageUrl() == null || request.imageUrl().isBlank()) {
			throw new BadRequestException(ErrorCode.IMAGE_URL_REQUIRED);
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		String ratio = request.ratio() == null || request.ratio().isBlank() ? "9:16" : request.ratio();

		VideoTemplate v = VideoTemplate.create(
			slugGenerator.generate(SLUG_PREFIX), user,
			"business", "cling", MediaType.VIDEO,
			"image-to-video", request.userPrompt(), null, null, ratio,
			request.imageUrl(), request.imagePath(),
			request.sourceJobId(), request.sourceItemId(), request.sourceKind()
		);
		v = repository.save(v);
		eventPublisher.publishEvent(new VideoTemplateCreatedEvent(v.getId()));
		log.info("[VideoTemplate] created videoId={} userId={}", v.getVideoSlug(), userId);
		return VideoJobCreatedResponse.of(v.getVideoSlug());
	}

	public VideoJobResponse get(Long userId, String videoId) {
		VideoTemplate v = repository.findByVideoSlug(videoId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.VIDEO_TEMPLATE_NOT_FOUND));
		if (!v.getUser().getId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}
		return VideoJobResponse.of(v);
	}
}
