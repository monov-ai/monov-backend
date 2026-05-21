package com.monovai.domain.template.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monovai.domain.template.dto.response.TemplateView;
import com.monovai.domain.template.entity.Template;
import com.monovai.domain.template.entity.TemplateFavorite;
import com.monovai.domain.template.repository.TemplateFavoriteRepository;
import com.monovai.domain.template.repository.TemplateRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateCatalogService {

	private static final Duration URL_TTL = Duration.ofDays(7);

	private final TemplateRepository templateRepository;
	private final TemplateFavoriteRepository favoriteRepository;
	private final S3Service s3Service;

	public List<TemplateView> list() {
		return templateRepository.findAllByOrderByCreatedAtDesc().stream()
			.map(t -> TemplateView.of(t, sign(t.getImagePath())))
			.toList();
	}

	public TemplateView get(String id) {
		Template t = templateRepository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
		return TemplateView.of(t, sign(t.getImagePath()));
	}

	@Transactional
	public long incrementDownload(String id) {
		Template t = templateRepository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
		t.incrementDownloadCount();
		return t.getDownloadCount();
	}

	public FavoriteList listFavorites(Long userId) {
		List<TemplateFavorite> favs = favoriteRepository.findAllByUserId(userId);
		Set<String> ids = new HashSet<>();
		favs.forEach(f -> ids.add(f.getTemplateId()));
		List<TemplateView> items = new ArrayList<>();
		for (String id : ids) {
			templateRepository.findById(id).ifPresent(t -> items.add(TemplateView.of(t, sign(t.getImagePath()))));
		}
		return new FavoriteList(items, new ArrayList<>(ids));
	}

	@Transactional
	public boolean toggleFavorite(Long userId, String templateId, boolean favorite) {
		var existing = favoriteRepository.findByUserIdAndTemplateId(userId, templateId);
		if (favorite) {
			if (existing.isEmpty()) favoriteRepository.save(TemplateFavorite.create(userId, templateId));
		} else {
			existing.ifPresent(favoriteRepository::delete);
		}
		return favorite;
	}

	private String sign(String key) {
		if (key == null || key.isBlank()) return null;
		return s3Service.getPreSignedUrlForDownload(key, URL_TTL);
	}

	public record FavoriteList(List<TemplateView> items, List<String> ids) {
	}
}
