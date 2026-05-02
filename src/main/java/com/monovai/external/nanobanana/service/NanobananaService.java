package com.monovai.external.nanobanana.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.monovai.external.nanobanana.client.NanobananaFeignClient;
import com.monovai.external.nanobanana.dto.NanobananaResult;
import com.monovai.external.nanobanana.dto.request.GenerateContentRequest;
import com.monovai.external.nanobanana.dto.request.GenerateContentRequest.ImageData;
import com.monovai.external.nanobanana.dto.response.GenerateContentResponse;
import com.monovai.external.nanobanana.properties.NanobananaProperties;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Nanobanana (Gemini 이미지 생성/편집) 호출 서비스.
 *
 * 흐름:
 *  1. sourceImageUrls 가 있으면 각각 HTTP fetch → bytes (multimodal 입력)
 *  2. Feign 으로 generateContent 호출 → base64 이미지 반환
 *  3. base64 디코드 → S3 업로드 → key 반환
 *  4. nanobananaTaskId 는 자체 발급 (Gemini 는 sync API 라 별도 taskId 없음)
 *
 * Phase 2: 단일 source (제품 사진).
 * Phase 3 edit: base 이미지 + 선택적 reference 이미지 (최대 2개).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NanobananaService {

	private static final String S3_KEY_PREFIX = "business_images";
	private static final String DEFAULT_INPUT_MIME = "image/jpeg";

	private final NanobananaFeignClient feignClient;
	private final NanobananaProperties properties;
	private final S3Service s3Service;

	private final RestClient restClient = RestClient.create();

	/**
	 * 0개 이상의 입력 이미지로 Gemini 호출.
	 * - 빈 배열 / null 만 → textOnly
	 * - 1개 → 단일 이미지 multimodal
	 * - 2개+ → 다중 이미지 multimodal (base + reference 등)
	 */
	public NanobananaResult generateImage(String prompt, String... sourceImageUrls) {
		log.info("[Nanobanana] generateImage prompt length={}, source images={}",
			prompt.length(), sourceImageUrls == null ? 0 : sourceImageUrls.length);

		GenerateContentRequest request = buildRequest(prompt, sourceImageUrls);

		GenerateContentResponse response;
		try {
			response = feignClient.generate(properties.getModel(), request);
		} catch (Exception e) {
			log.error("[Nanobanana] API 호출 실패", e);
			throw new RuntimeException("Nanobanana API 호출 실패: " + e.getMessage(), e);
		}

		GenerateContentResponse.InlineData image = response.firstInlineImage()
			.orElseThrow(() -> new RuntimeException("Nanobanana 응답에 이미지가 없습니다"));

		byte[] imageBytes = Base64.getDecoder().decode(image.data());
		String mimeType = image.mimeType() != null ? image.mimeType() : "image/png";
		String extension = mimeTypeToExtension(mimeType);
		String taskId = "nb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String key = S3_KEY_PREFIX + "/" + taskId + "." + extension;

		s3Service.uploadBytes(key, imageBytes, mimeType);
		log.info("[Nanobanana] 이미지 저장 완료: taskId={}, key={}, bytes={}",
			taskId, key, imageBytes.length);

		return new NanobananaResult(taskId, key);
	}

	private GenerateContentRequest buildRequest(String prompt, String[] sourceImageUrls) {
		List<ImageData> fetched = new ArrayList<>();
		if (sourceImageUrls != null) {
			for (String url : sourceImageUrls) {
				if (url == null || url.isBlank()) {
					continue;
				}
				fetched.add(fetchImage(url));
			}
		}

		if (fetched.isEmpty()) {
			log.info("[Nanobanana] 입력 이미지 없음 → textOnly 모드");
			return GenerateContentRequest.textOnly(prompt);
		}
		log.info("[Nanobanana] 입력 이미지 {}개 multimodal", fetched.size());
		return GenerateContentRequest.textWithImages(prompt, fetched);
	}

	private ImageData fetchImage(String url) {
		try {
			ResponseEntity<byte[]> imageResponse = restClient.get()
				.uri(URI.create(url))
				.retrieve()
				.toEntity(byte[].class);

			byte[] bytes = imageResponse.getBody();
			if (bytes == null || bytes.length == 0) {
				throw new IllegalStateException("입력 이미지 fetch 결과 비어있음");
			}

			String mimeType = Optional.ofNullable(imageResponse.getHeaders().getContentType())
				.map(MediaType::toString)
				.map(NanobananaService::stripCharset)
				.filter(NanobananaService::isSupportedInputMime)
				.orElse(DEFAULT_INPUT_MIME);

			log.info("[Nanobanana] sourceImage fetched: bytes={}, mime={}", bytes.length, mimeType);
			return new ImageData(bytes, mimeType);
		} catch (Exception e) {
			log.error("[Nanobanana] sourceImage fetch 실패: {}", url, e);
			throw new RuntimeException("입력 이미지 fetch 실패: " + e.getMessage(), e);
		}
	}

	private String mimeTypeToExtension(String mimeType) {
		return switch (mimeType.toLowerCase()) {
			case "image/png" -> "png";
			case "image/jpeg", "image/jpg" -> "jpg";
			case "image/webp" -> "webp";
			default -> "bin";
		};
	}

	private static String stripCharset(String value) {
		int semicolonIdx = value.indexOf(';');
		return (semicolonIdx >= 0 ? value.substring(0, semicolonIdx) : value).trim();
	}

	private static boolean isSupportedInputMime(String mime) {
		return switch (mime.toLowerCase()) {
			case "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif" -> true;
			default -> false;
		};
	}
}
