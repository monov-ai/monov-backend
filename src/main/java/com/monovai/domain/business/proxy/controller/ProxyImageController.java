package com.monovai.domain.business.proxy.controller;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BadRequestException;
import com.monovai.global.error.exception.BusinessException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인페인팅 모달이 캔버스에서 base 이미지 + mask 를 export 할 때 CORS tainted 가 되는 문제를 우회.
 * SSRF 방지: 인증 필수, HTTPS-only, 호스트 화이트리스트, Content-Type=image/*.
 */
@Tag(name = "Business / Proxy Image", description = "SSRF-safe 외부 이미지 프록시 (캔버스 export 용)")
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@Slf4j
public class ProxyImageController {

	private static final long MAX_BYTES = 25L * 1024 * 1024;

	private static final String[] EXACT_HOSTS = {
		"firebasestorage.googleapis.com",
		"storage.googleapis.com",
		"storage.cloud.google.com"
	};

	private static final String[] SUFFIX_HOSTS = {
		".firebasestorage.app",
		".googleusercontent.com",
		".amazonaws.com"
	};

	private final RestClient http = RestClient.create();

	@GetMapping("/proxy-image")
	@Operation(
		summary = "외부 이미지 프록시 (SSRF-safe)",
		description = """
			인페인팅 모달이 base 이미지를 canvas 에 그려 mask 와 함께 export 할 때, Firebase signed URL 을 그대로 쓰면
			CORS tainted 가 돼서 canvas export 가 실패합니다. 이 endpoint 가 우리 origin 으로 한 번 우회시켜 줍니다.

			**보안 규칙:**
			- 인증 필수 (SSRF 방지)
			- HTTPS only
			- 호스트 화이트리스트: `firebasestorage.googleapis.com`, `storage.googleapis.com`, `storage.cloud.google.com`, `*.firebasestorage.app`, `*.googleusercontent.com`, `*.amazonaws.com`
			- Upstream `Content-Type` 이 `image/*` 인 응답만
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이미지 바이너리"),
		@ApiResponse(responseCode = "400", description = "url 누락 / 잘못된 URL / HTTPS 아님 / 호스트 차단"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "415", description = "이미지 아님"),
		@ApiResponse(responseCode = "502", description = "Upstream 실패")
	})
	public ResponseEntity<byte[]> proxy(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "프록시할 외부 이미지 URL (encoded)")
		@RequestParam(value = "url", required = false) String url
	) {
		if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
		if (url == null || url.isBlank()) throw new BadRequestException(ErrorCode.PROXY_URL_REQUIRED);

		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new BadRequestException(ErrorCode.PROXY_URL_INVALID);
		}
		String scheme = uri.getScheme();
		if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
			throw new BadRequestException(ErrorCode.PROXY_HTTPS_REQUIRED);
		}
		String host = uri.getHost();
		if (host == null || !isAllowedHost(host)) {
			throw new BadRequestException(ErrorCode.PROXY_HOST_NOT_ALLOWED);
		}

		ResponseEntity<byte[]> res;
		try {
			res = http.get().uri(uri).retrieve().toEntity(byte[].class);
		} catch (Exception e) {
			log.warn("[proxy-image] upstream 실패: {}", e.getMessage());
			throw new BusinessException(ErrorCode.PROXY_UPSTREAM_FAILED);
		}
		MediaType contentType = res.getHeaders().getContentType();
		if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {
			throw new BusinessException(ErrorCode.PROXY_NOT_IMAGE);
		}
		byte[] body = res.getBody();
		if (body == null || body.length == 0) {
			throw new BusinessException(ErrorCode.PROXY_UPSTREAM_FAILED);
		}
		if (body.length > MAX_BYTES) {
			throw new BusinessException(ErrorCode.PROXY_UPSTREAM_FAILED);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(contentType);
		headers.setContentLength(body.length);
		headers.setCacheControl("private, max-age=300");
		return new ResponseEntity<>(body, headers, 200);
	}

	private boolean isAllowedHost(String host) {
		String h = host.toLowerCase();
		for (String exact : EXACT_HOSTS) if (h.equals(exact)) return true;
		for (String suffix : SUFFIX_HOSTS) if (h.endsWith(suffix)) return true;
		return false;
	}
}
