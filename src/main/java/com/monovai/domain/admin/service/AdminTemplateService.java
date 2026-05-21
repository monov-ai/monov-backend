package com.monovai.domain.admin.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.monovai.domain.template.dto.response.TemplateView;
import com.monovai.domain.template.entity.Template;
import com.monovai.domain.template.repository.TemplateRepository;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTemplateService {

	private final TemplateRepository templateRepository;
	private final S3Service s3Service;

	public List<Template> list() {
		return templateRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional
	public Template create(Map<String, Object> body) {
		String id = str(body.get("id"));
		if (id == null || id.isBlank()) {
			id = "tpl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		}
		Template t = Template.builder()
			.id(id)
			.title(str(body.get("title")))
			.shortDescription(str(body.get("shortDescription")))
			.category(str(body.get("category")))
			.credit(intOf(body.get("credit")))
			.mediaType(str(body.get("mediaType")))
			.model(str(body.get("model")))
			.imagePath(str(body.get("imagePath")))
			.imageUrl(str(body.get("imageUrl")))
			.prompt(str(body.get("prompt")))
			.promptGuide(str(body.get("promptGuide")))
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		Object tags = body.get("tags");
		if (tags instanceof List<?> list) {
			for (Object tag : list) if (tag != null) t.addHashtag(tag.toString());
		}
		return templateRepository.save(t);
	}

	@Transactional
	public Template update(String id, Map<String, Object> body) {
		Template existing = templateRepository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
		// 부분 갱신을 위해 빌더로 재구성 (id 유지)
		Template t = Template.builder()
			.id(existing.getId())
			.title(strOr(body.get("title"), existing.getTitle()))
			.shortDescription(strOr(body.get("shortDescription"), existing.getShortDescription()))
			.category(strOr(body.get("category"), existing.getCategory()))
			.credit(body.containsKey("credit") ? intOf(body.get("credit")) : existing.getCredit())
			.mediaType(strOr(body.get("mediaType"), existing.getMediaType()))
			.model(strOr(body.get("model"), existing.getModel()))
			.imagePath(strOr(body.get("imagePath"), existing.getImagePath()))
			.imageUrl(strOr(body.get("imageUrl"), existing.getImageUrl()))
			.prompt(strOr(body.get("prompt"), existing.getPrompt()))
			.promptGuide(strOr(body.get("promptGuide"), existing.getPromptGuide()))
			.createdAt(existing.getCreatedAt())
			.updatedAt(LocalDateTime.now())
			.build();
		templateRepository.delete(existing);
		return templateRepository.save(t);
	}

	@Transactional
	public void delete(String id) {
		Template t = templateRepository.findById(id)
			.orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
		templateRepository.delete(t);
	}

	public Map<String, Object> stats() {
		List<Template> all = templateRepository.findAll();
		long totalDownloads = all.stream().mapToLong(Template::getDownloadCount).sum();
		long totalUsages = all.stream().mapToLong(Template::getUsageCount).sum();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("totalTemplates", all.size());
		body.put("totalDownloads", totalDownloads);
		body.put("totalUsages", totalUsages);
		return body;
	}

	public Map<String, Object> upload(MultipartFile file) {
		if (file == null || file.isEmpty()) throw new BadRequestException(ErrorCode.MISSING_FILE);
		String safe = file.getOriginalFilename() == null ? "template"
			: file.getOriginalFilename().replaceAll("[^A-Za-z0-9._-]", "_");
		String key = "templates/" + System.currentTimeMillis() + "_" + safe;
		try {
			s3Service.uploadBytes(key, file.getBytes(),
				file.getContentType() == null ? "application/octet-stream" : file.getContentType());
		} catch (IOException e) {
			throw new BadRequestException(ErrorCode.FILE_UPLOAD_FAIL);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("storagePath", key);
		body.put("url", s3Service.getPreSignedUrlForDownload(key, java.time.Duration.ofDays(7)));
		return body;
	}

	public TemplateView toView(Template t) {
		String url = t.getImagePath() != null
			? s3Service.getPreSignedUrlForDownload(t.getImagePath(), java.time.Duration.ofDays(7)) : null;
		return TemplateView.of(t, url);
	}

	private static String str(Object o) {
		return o == null ? null : o.toString();
	}

	private static String strOr(Object o, String fallback) {
		return o == null ? fallback : o.toString();
	}

	private static int intOf(Object o) {
		if (o instanceof Number n) return n.intValue();
		if (o instanceof String s && !s.isBlank()) {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException ignored) {
			}
		}
		return 0;
	}
}
