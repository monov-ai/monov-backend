package com.monovai.domain.template.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.business.video.entity.VideoTemplate;
import com.monovai.domain.business.video.entity.enums.MediaType;
import com.monovai.domain.business.video.repository.VideoTemplateRepository;
import com.monovai.domain.credit.CreditPolicy;
import com.monovai.domain.credit.service.CreditService;
import com.monovai.domain.template.dto.request.TemplateRequestDto;
import com.monovai.domain.template.entity.Template;
import com.monovai.domain.template.repository.TemplateRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.common.util.SlugGenerator;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.worker.business.event.VideoTemplateCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * C.1 POST /template-requests. image/video 잡 큐잉 + 크레딧 차감.
 * studio/templates 흐름은 크레딧 차감, business 흐름은 무료.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateRequestService {

	private static final String SLUG_PREFIX = "mvit";

	private final VideoTemplateRepository videoRepository;
	private final TemplateRepository templateRepository;
	private final UserRepository userRepository;
	private final CreditService creditService;
	private final SlugGenerator slugGenerator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public String create(Long userId, TemplateRequestDto req) {
		if (req.imageUrl() == null || req.imageUrl().isBlank()) {
			throw new BadRequestException(ErrorCode.TEMPLATE_REQUEST_INVALID);
		}
		MediaType mediaType = MediaType.from(req.mediaType());
		String source = req.source() == null ? "templates" : req.source();
		boolean isBusiness = "business".equalsIgnoreCase(source);

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		// studio 등 유료 흐름은 templatePrompt 필수 + 크레딧 차감
		if (!isBusiness && (req.templatePrompt() == null || req.templatePrompt().isBlank())) {
			throw new BadRequestException(ErrorCode.TEMPLATE_REQUEST_INVALID);
		}

		String videoSlug = req.videoId() != null && !req.videoId().isBlank()
			? req.videoId() : slugGenerator.generate(SLUG_PREFIX);

		String model = req.model() != null ? req.model() : (mediaType == MediaType.VIDEO ? "cling" : "nanobanana");
		String ratio = req.ratio() != null ? req.ratio() : "9:16";

		VideoTemplate vt = VideoTemplate.create(
			videoSlug, user, source, model, mediaType,
			req.title(), req.userPrompt(), req.templatePrompt(), req.templateId(), ratio,
			req.imageUrl(), req.imagePath(),
			null, null, null
		);
		vt = videoRepository.save(vt);

		// 크레딧 차감 (business 무료) — 멱등키 = videoSlug
		if (!isBusiness) {
			int cost = mediaType == MediaType.VIDEO ? CreditPolicy.VIDEO_PER_UNIT : CreditPolicy.IMAGE_PER_UNIT;
			creditService.deduct(userId, cost, "템플릿 적용: " + req.templateId(), "tpl_req:" + videoSlug);
		}

		// 템플릿 사용 카운트 증가
		if (req.templateId() != null) {
			templateRepository.findById(req.templateId()).ifPresent(Template::incrementUsageCount);
		}

		eventPublisher.publishEvent(new VideoTemplateCreatedEvent(vt.getId()));
		log.info("[TemplateRequest] created videoId={} source={} media={}", videoSlug, source, mediaType);
		return videoSlug;
	}
}
