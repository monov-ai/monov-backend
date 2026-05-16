package com.monovai.external.openai.service;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.monovai.external.nanobanana.dto.NanobananaResult;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI gpt-image-1 이미지 편집 (인페인팅) 동기 호출.
 *
 * 흐름:
 *  1. base 이미지 (HTTP fetch → bytes) + mask (이미 우리가 가진 bytes) 를 multipart 로 OpenAI 에 POST
 *  2. base64 응답 → S3 업로드 → key 반환
 *
 * 단순 구현: sharp letterbox 패딩 / hard composite 는 v2.0 정밀 흐름이라 별도 단계. 현재는 OpenAI 응답을
 * 그대로 사용 (마스크 밖 drift 는 다음 phase 에서 보강).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiImageEditService {

	private static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/images/edits";
	private static final String MODEL = "gpt-image-1";
	private static final String S3_KEY_PREFIX = "business_images";

	private final S3Service s3Service;

	@Value("${spring.ai.openai.api-key}")
	private String apiKey;

	private final RestClient http = RestClient.create();

	public NanobananaResult inpaint(String prompt, String baseImageUrl, byte[] maskPngBytes, String size) {
		byte[] baseBytes = fetchBytes(baseImageUrl);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("model", MODEL);
		body.add("prompt", prompt);
		body.add("size", normalizeSize(size));
		body.add("image", asResource(baseBytes, "base.png"));
		body.add("mask", asResource(maskPngBytes, "mask.png"));

		Map<String, Object> response;
		try {
			response = http.post()
				.uri(URI.create(OPENAI_ENDPOINT))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(Map.class);
		} catch (Exception e) {
			log.error("[OpenAI/inpaint] 호출 실패", e);
			throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);
		}
		if (response == null) throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);

		Object dataObj = response.get("data");
		if (!(dataObj instanceof List<?> list) || list.isEmpty()) {
			throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);
		}
		Object first = list.get(0);
		if (!(first instanceof Map<?, ?> m)) {
			throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);
		}
		Object b64 = m.get("b64_json");
		if (!(b64 instanceof String b64s)) {
			throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);
		}
		byte[] result = Base64.getDecoder().decode(b64s);

		String taskId = "oai_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String key = S3_KEY_PREFIX + "/" + taskId + ".png";
		s3Service.uploadBytes(key, result, "image/png");
		log.info("[OpenAI/inpaint] saved: taskId={}, key={}, bytes={}", taskId, key, result.length);
		return new NanobananaResult(taskId, key);
	}

	private byte[] fetchBytes(String url) {
		try {
			ResponseEntity<byte[]> res = http.get().uri(URI.create(url)).retrieve().toEntity(byte[].class);
			byte[] bytes = res.getBody();
			if (bytes == null || bytes.length == 0) {
				throw new IllegalStateException("base 이미지 fetch 빈 응답");
			}
			return bytes;
		} catch (Exception e) {
			log.error("[OpenAI/inpaint] base fetch 실패: {}", url, e);
			throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);
		}
	}

	private static Resource asResource(byte[] data, String filename) {
		return new ByteArrayResource(data) {
			@Override
			public String getFilename() {
				return filename;
			}
		};
	}

	private static String normalizeSize(String size) {
		if (size == null || size.isBlank() || "auto".equalsIgnoreCase(size)) return "auto";
		return switch (size) {
			case "1024x1024", "1024x1536", "1536x1024" -> size;
			default -> "auto";
		};
	}
}
