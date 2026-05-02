package com.monovai.external.nanobanana.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.monovai.external.nanobanana.config.NanobananaFeignConfig;
import com.monovai.external.nanobanana.dto.request.GenerateContentRequest;
import com.monovai.external.nanobanana.dto.response.GenerateContentResponse;

@FeignClient(
	name = "nanobananaFeignClient",
	url = "${nanobanana.base-url:https://generativelanguage.googleapis.com}",
	configuration = NanobananaFeignConfig.class
)
public interface NanobananaFeignClient {

	/**
	 * Gemini generateContent — 모델명을 path 에 끼워 넣는다.
	 * 예: /v1beta/models/gemini-2.5-flash-image-preview:generateContent
	 */
	@PostMapping("/v1beta/models/{model}:generateContent")
	GenerateContentResponse generate(
		@PathVariable("model") String model,
		@RequestBody GenerateContentRequest request
	);
}