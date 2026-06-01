package com.monovai.domain.admin.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.credit.service.CreditService;
import com.monovai.domain.feedback.service.UserFeedbackService;
import com.monovai.domain.template.repository.TemplateRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * C.4 어드민 집계. Firestore aggregation 화면을 RDB 집계로 대체.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

	private final UserRepository userRepository;
	private final TemplateRepository templateRepository;
	private final CreditService creditService;
	private final UserFeedbackService userFeedbackService;

	public Map<String, Object> dashboard() {
		long totalUsers = userRepository.count();
		long totalTemplates = templateRepository.count();
		long newUsers7d = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7));

		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("totalUsers", totalUsers);
		summary.put("totalTemplates", totalTemplates);
		summary.put("newUsers7d", newUsers7d);

		List<Map<String, Object>> topTemplates = templateRepository.findAll().stream()
			.sorted((a, b) -> Long.compare(b.getUsageCount(), a.getUsageCount()))
			.limit(10)
			.map(t -> {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", t.getId());
				m.put("title", t.getTitle());
				m.put("usageCount", t.getUsageCount());
				m.put("downloadCount", t.getDownloadCount());
				return m;
			})
			.toList();

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("summary", summary);
		body.put("signupTrend", List.of());  // 일자별 추세 — 추후 집계 쿼리로 확장
		body.put("topTemplates", topTemplates);
		return body;
	}

	public Map<String, Object> feedback() {
		return userFeedbackService.listForAdmin(200);
	}

	public Map<String, Object> users(String search) {
		List<User> users;
		if (search != null && !search.isBlank()) {
			users = userRepository.findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
				search, search, PageRequest.of(0, 100));
		} else {
			users = userRepository.findAll(PageRequest.of(0, 100)).getContent();
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (User u : users) list.add(userBrief(u));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("users", list);
		body.put("signupTrend", List.of());
		return body;
	}

	public Map<String, Object> userDetail(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
		var wallet = creditService.getWallet(userId);

		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("credits", wallet != null ? wallet.getLegacyCredits() : 0);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("user", userBrief(user));
		body.put("summary", summary);
		body.put("activities", List.of());
		body.put("feedbacks", List.of());
		return body;
	}

	@Transactional
	public Map<String, Object> adjustCredits(Long userId, int deltaLegacy, int deltaAdImage,
		int deltaAiVideo, int deltaImageToVideo) {
		userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
		creditService.adjust(userId, deltaLegacy, deltaAdImage, deltaAiVideo, deltaImageToVideo,
			"관리자 크레딧 조정");
		var wallet = creditService.getWallet(userId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("credits", wallet != null ? wallet.getLegacyCredits() : 0);
		return body;
	}

	private Map<String, Object> userBrief(User u) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", u.getId());
		m.put("email", u.getEmail());
		m.put("name", u.getName());
		m.put("nickname", u.getNickname());
		m.put("role", u.getRole() != null ? u.getRole().name() : null);
		m.put("active", u.getActive() == null || u.getActive());
		m.put("createdAt", u.getCreatedAt());
		return m;
	}
}
