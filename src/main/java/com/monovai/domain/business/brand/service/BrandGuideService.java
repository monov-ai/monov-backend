package com.monovai.domain.business.brand.service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.monovai.domain.business.brand.dto.request.CreateBrandGuideRequest;
import com.monovai.domain.business.brand.dto.request.UpdateBrandGuideRequest;
import com.monovai.domain.business.brand.dto.response.BrandGuideListResponse;
import com.monovai.domain.business.brand.dto.response.BrandGuideResponse;
import com.monovai.domain.business.brand.dto.response.BrandUploadResponse;
import com.monovai.domain.business.brand.entity.BrandGuide;
import com.monovai.domain.business.brand.repository.BrandGuideRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.global.common.util.SlugGenerator;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.ForbiddenException;
import com.monovai.global.error.exception.NotFoundException;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BrandGuideService {

	private static final String SLUG_PREFIX = "brand";
	private static final long MAX_UPLOAD_BYTES = 25L * 1024 * 1024;
	private static final Duration UPLOAD_URL_TTL = Duration.ofDays(7);

	private final BrandGuideRepository repository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final S3Service s3Service;

	public BrandGuideListResponse list(Long userId) {
		List<BrandGuide> guides = repository.findAllByUser_IdOrderByIsDefaultDescCreatedAtDesc(userId);
		return BrandGuideListResponse.of(guides.stream().map(BrandGuideResponse.GuideView::of).toList());
	}

	public BrandGuideResponse get(Long userId, String guideId) {
		BrandGuide g = repository.findByGuideSlug(guideId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_GUIDE_NOT_FOUND));
		if (!g.getUser().getId().equals(userId)) throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		return BrandGuideResponse.of(g);
	}

	@Transactional
	public BrandGuideResponse create(Long userId, CreateBrandGuideRequest req) {
		if (req.name() == null || req.name().isBlank()) {
			throw new BadRequestException(ErrorCode.BRAND_GUIDE_NAME_REQUIRED);
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		long existing = repository.countByUser_Id(userId);
		boolean shouldBeDefault = Boolean.TRUE.equals(req.isDefault()) || existing == 0;
		if (shouldBeDefault) clearOtherDefaults(userId, null);

		BrandGuide g = BrandGuide.create(
			slugGenerator.generate(SLUG_PREFIX), user, req.name(), shouldBeDefault, req.description()
		);
		g = repository.save(g);
		return BrandGuideResponse.of(g);
	}

	@Transactional
	public BrandGuideResponse update(Long userId, String guideId, UpdateBrandGuideRequest req) {
		BrandGuide g = repository.findByGuideSlug(guideId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_GUIDE_NOT_FOUND));
		if (!g.getUser().getId().equals(userId)) throw new ForbiddenException(ErrorCode.ACCESS_DENIED);

		if (req.name() != null && !req.name().isBlank()) g.rename(req.name());
		if (req.description() != null) g.setDescription(req.description());
		if (req.identity() != null) g.setIdentity(req.identity());
		if (req.palette() != null) g.setPalette(req.palette());
		if (req.guidelines() != null) g.setGuidelines(req.guidelines());
		if (req.assets() != null) g.setAssets(req.assets());
		if (req.recentUsages() != null) g.setRecentUsages(req.recentUsages());
		if (Boolean.TRUE.equals(req.isDefault())) {
			clearOtherDefaults(userId, g.getId());
			g.setIsDefault(true);
		} else if (Boolean.FALSE.equals(req.isDefault())) {
			g.setIsDefault(false);
		}
		return BrandGuideResponse.of(g);
	}

	@Transactional
	public void delete(Long userId, String guideId) {
		BrandGuide g = repository.findByGuideSlug(guideId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_GUIDE_NOT_FOUND));
		if (!g.getUser().getId().equals(userId)) throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		repository.delete(g);
	}

	@Transactional
	public BrandGuideResponse setAsDefault(Long userId, String guideId) {
		BrandGuide g = repository.findByGuideSlug(guideId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_GUIDE_NOT_FOUND));
		if (!g.getUser().getId().equals(userId)) throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		clearOtherDefaults(userId, g.getId());
		g.setIsDefault(true);
		return BrandGuideResponse.of(g);
	}

	public BrandUploadResponse upload(Long userId, String guideId, MultipartFile file, String kind, String name) {
		BrandGuide g = repository.findByGuideSlug(guideId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_GUIDE_NOT_FOUND));
		if (!g.getUser().getId().equals(userId)) throw new ForbiddenException(ErrorCode.ACCESS_DENIED);

		if (file == null || file.isEmpty()) throw new BadRequestException(ErrorCode.BRAND_GUIDE_FILE_MISSING);
		if (file.getSize() > MAX_UPLOAD_BYTES) throw new BadRequestException(ErrorCode.BRAND_GUIDE_FILE_TOO_LARGE);

		String safeKind = (kind == null ? "" : kind).toLowerCase(Locale.ROOT);
		Set<String> allowed = allowedMimes(safeKind);
		if (allowed == null) throw new BadRequestException(ErrorCode.BRAND_GUIDE_KIND_INVALID);

		String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
		String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
		String ext = extensionOf(originalName);
		if (!allowed.contains(contentType) && !isFontByExtension(safeKind, ext)) {
			throw new BadRequestException(ErrorCode.BRAND_GUIDE_FILE_TYPE_INVALID);
		}

		String id = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
		String folder = switch (safeKind) {
			case "logo" -> "logos";
			case "mood" -> "moods";
			case "font" -> "fonts";
			default -> "assets";
		};
		String safeName = sanitize(originalName);
		String key = "brand_guides/" + userId + "/" + g.getGuideSlug() + "/" + folder + "/" + id + "_" + safeName;

		try {
			s3Service.uploadBytes(key, file.getBytes(), contentType.isBlank() ? "application/octet-stream" : contentType);
		} catch (IOException e) {
			throw new BadRequestException(ErrorCode.FILE_UPLOAD_FAIL);
		}
		String url = s3Service.getPreSignedUrlForDownload(key, UPLOAD_URL_TTL);

		return new BrandUploadResponse(
			true, id, url, key,
			name != null && !name.isBlank() ? name : originalName,
			ext.toUpperCase(Locale.ROOT),
			file.getSize(), safeKind
		);
	}

	private void clearOtherDefaults(Long userId, Long exceptId) {
		List<BrandGuide> all = repository.findAllByUser_Id(userId);
		for (BrandGuide g : all) {
			if (exceptId != null && g.getId().equals(exceptId)) continue;
			if (g.isDefault()) g.setIsDefault(false);
		}
	}

	private Set<String> allowedMimes(String kind) {
		return switch (kind) {
			case "logo" -> Set.of("image/svg+xml", "image/png", "image/jpeg", "image/webp");
			case "mood" -> Set.of("image/png", "image/jpeg", "image/webp");
			case "asset" -> Set.of(
				"image/png", "image/jpeg", "image/webp", "image/svg+xml",
				"application/pdf", "application/postscript", "application/illustrator"
			);
			case "font" -> Set.of(
				"font/ttf", "font/otf", "font/woff", "font/woff2",
				"application/x-font-ttf", "application/x-font-otf", "application/x-font-woff",
				"application/font-woff", "application/font-woff2", "application/octet-stream"
			);
			default -> null;
		};
	}

	private boolean isFontByExtension(String kind, String ext) {
		if (!"font".equals(kind)) return false;
		Set<String> allowedExt = new HashSet<>(List.of("ttf", "otf", "woff", "woff2"));
		return allowedExt.contains(ext.toLowerCase(Locale.ROOT));
	}

	private String extensionOf(String filename) {
		int dot = filename.lastIndexOf('.');
		return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
	}

	private String sanitize(String name) {
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}
}
