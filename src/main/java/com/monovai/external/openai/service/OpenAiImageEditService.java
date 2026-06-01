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
 * OpenAI 이미지 편집/생성 동기 호출.
 *
 * 모델 정책 (memory-noted 2026-05-27):
 *  - inpaint (mask + base 필수)         → gpt-image-1
 *  - source-image + transparentBackground → gpt-image-1 (배경 투명 지원은 gpt-image-1 만)
 *  - 그 외 (freeform / source-image / text_create("수정하기")) → gpt-image-2
 *
 * 흐름:
 *  1. base 이미지 (HTTP fetch → bytes) + mask (있으면) 를 multipart 로 OpenAI 에 POST
 *  2. base64 응답 → S3 업로드 → key 반환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiImageEditService {

	private static final String EDITS_ENDPOINT = "https://api.openai.com/v1/images/edits";
	private static final String GENERATIONS_ENDPOINT = "https://api.openai.com/v1/images/generations";
	public static final String MODEL_GPT_IMAGE_1 = "gpt-image-1";
	public static final String MODEL_GPT_IMAGE_2 = "gpt-image-2";
	private static final String S3_KEY_PREFIX = "business_images";

	private final S3Service s3Service;

	@Value("${spring.ai.openai.api-key}")
	private String apiKey;

	private final RestClient http = RestClient.create();

	/** inpaint 는 항상 gpt-image-1 (mask 지원 모델). transparentBackground 옵션도 여기서만 의미. */
	public NanobananaResult inpaint(String prompt, String baseImageUrl, byte[] maskPngBytes, String size) {
		return inpaint(prompt, baseImageUrl, maskPngBytes, size, false);
	}

	public NanobananaResult inpaint(String prompt, String baseImageUrl, byte[] maskPngBytes, String size,
		boolean transparentBackground) {
		byte[] baseBytes = fetchBytes(baseImageUrl);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("model", MODEL_GPT_IMAGE_1);
		body.add("prompt", prompt);
		body.add("size", normalizeSize(size));
		body.add("image", asResource(baseBytes, "base.png"));
		body.add("mask", asResource(maskPngBytes, "mask.png"));
		if (transparentBackground) {
			body.add("background", "transparent");
			body.add("output_format", "png");
		}

		Map<String, Object> response;
		try {
			response = http.post()
				.uri(URI.create(EDITS_ENDPOINT))
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

	/**
	 * 소스이미지 기반 편집/생성 (mask 없음). transparentBackground 가 true 면 gpt-image-1,
	 * false 면 gpt-image-2 사용. base 이미지 URL 1장만 지원. 결과는 S3 업로드 후 key 반환.
	 */
	public NanobananaResult editWithSource(String prompt, String baseImageUrl, String size,
		boolean transparentBackground) {
		byte[] baseBytes = fetchBytes(baseImageUrl);
		String model = transparentBackground ? MODEL_GPT_IMAGE_1 : MODEL_GPT_IMAGE_2;

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("model", model);
		body.add("prompt", prompt);
		body.add("size", normalizeSize(size));
		body.add("image", asResource(baseBytes, "base.png"));
		if (transparentBackground) {
			body.add("background", "transparent");
			body.add("output_format", "png");
		}

		Map<String, Object> response = postOrThrow(EDITS_ENDPOINT, body);
		byte[] result = decodeFirstB64(response);
		return uploadResult(result);
	}

	/**
	 * 자유 생성 (base 이미지 없음). gpt-image-2 고정. transparent 가 필요한 경우 호출 측에서
	 * editWithSource 로 라우팅 (gpt-image-1 만 background:transparent 지원).
	 */
	public NanobananaResult generate(String prompt, String size) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("model", MODEL_GPT_IMAGE_2);
		body.add("prompt", prompt);
		body.add("size", normalizeSize(size));

		Map<String, Object> response = postOrThrow(GENERATIONS_ENDPOINT, body);
		byte[] result = decodeFirstB64(response);
		return uploadResult(result);
	}

	private Map<String, Object> postOrThrow(String endpoint, MultiValueMap<String, Object> body) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> res = http.post()
				.uri(URI.create(endpoint))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(Map.class);
			if (res == null) throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);
			return res;
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("[OpenAI] 호출 실패 endpoint={}", endpoint, e);
			throw new BusinessException(ErrorCode.OPENAI_IMAGE_EDIT_FAILED);
		}
	}

	private static byte[] decodeFirstB64(Map<String, Object> response) {
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
		return Base64.getDecoder().decode(b64s);
	}

	private NanobananaResult uploadResult(byte[] result) {
		String taskId = "oai_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String key = S3_KEY_PREFIX + "/" + taskId + ".png";
		s3Service.uploadBytes(key, result, "image/png");
		log.info("[OpenAI] saved: taskId={}, key={}, bytes={}", taskId, key, result.length);
		return new NanobananaResult(taskId, key);
	}
}
