package com.monovai.domain.feedback.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.feedback.dto.request.SubmitFeedbackRequest;
import com.monovai.domain.feedback.entity.UserFeedback;
import com.monovai.domain.feedback.repository.UserFeedbackRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserFeedbackService {

	private final UserFeedbackRepository feedbackRepository;
	private final UserRepository userRepository;

	@Transactional
	public void submit(Long userId, SubmitFeedbackRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		UserFeedback fb = UserFeedback.of(
			user, request.type(), request.mediaType(),
			request.templateId(), request.templateTitle(), request.itemId(),
			request.downloadSource(), request.rating(), request.tag(), request.note()
		);
		feedbackRepository.save(fb);
		log.info("[Feedback] saved userId={} type={} rating={}", userId, request.type(), request.rating());
	}

	/** Admin 대시보드용. 최근 N건 + 요약. */
	public Map<String, Object> listForAdmin(int limit) {
		List<UserFeedback> recent = feedbackRepository.findAllByOrderByCreatedAtDesc(
			PageRequest.of(0, Math.max(1, Math.min(limit, 500))));

		long total = feedbackRepository.count();
		double avgRating = recent.stream().mapToInt(UserFeedback::getRating).average().orElse(0.0);

		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("total", total);
		summary.put("avgRating", avgRating);

		List<Map<String, Object>> items = recent.stream().map(this::view).toList();

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("summary", summary);
		body.put("items", items);
		return body;
	}

	private Map<String, Object> view(UserFeedback f) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", f.getId());
		m.put("userId", f.getUser().getId());
		m.put("type", f.getType());
		m.put("mediaType", f.getMediaType());
		m.put("templateId", f.getTemplateId());
		m.put("templateTitle", f.getTemplateTitle());
		m.put("itemId", f.getItemId());
		m.put("downloadSource", f.getDownloadSource());
		m.put("rating", f.getRating());
		m.put("tag", f.getTag());
		m.put("note", f.getNote());
		m.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toInstant() : null);
		return m;
	}
}
