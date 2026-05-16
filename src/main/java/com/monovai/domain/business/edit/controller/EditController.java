package com.monovai.domain.business.edit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.edit.dto.request.EditImageRequest;
import com.monovai.domain.business.edit.dto.request.InpaintRequest;
import com.monovai.domain.business.edit.dto.response.EditCreatedResponse;
import com.monovai.domain.business.edit.service.EditService;
import com.monovai.domain.business.edit.service.InpaintService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Business / Edits", description = "결과 이미지 빠른 수정 + 인페인팅 (Nanobanana + OpenAI gpt-image-1)")
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class EditController {

	private final EditService editService;
	private final InpaintService inpaintService;

	@PostMapping("/edit-image")
	@Operation(
		summary = "빠른 수정 적용 (체이닝 가능)",
		description = """
			결과 이미지에 빠른 수정을 적용합니다. `baseId` 는 variantId(예: V1) 또는 같은 잡의 다른 editId 가 될 수 있어 체이닝이 가능합니다.

			**지원 mode (v2.0):**
			- `background_change` — `{ description?, referenceImageUrls?: ≤4, referenceImagePaths? }` 둘 중 1개 이상 필수. description 있으면 GPT compile 후 Nanobanana.
			- `lighting_change` — `{ lighting: "natural"|"warm"|"soft"|"strong" }`
			- `angle_change` — v2.0 좌표 `{ rotation: -180..180, tilt: -90..90 }` (둘 다 0 거부) 또는 legacy `{ angle: front|45|side|top }`
			- `ratio_change` — `{ ratio: 1:1|9:16|4:3|3:4 }`
			- `product_replace` — `{ referenceImageUrls: 1~4 }`
			- `object_add` — `{ description?, referenceImageUrls?: ≤4 }`
			- `text_create` — `{ description: 1~500자 }` base 없이 자유 생성. POSE_LOCK 적용 안 함.

			> `inpaint` 는 별도 endpoint `/api/v1/business/inpaint` 만 허용 (마스크 PNG 업로드가 필요).
			""",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = {
				@ExampleObject(name = "background_change (다중 reference)", value = """
					{
					  "jobId": "bizimg_...",
					  "baseId": "V1",
					  "mode": "background_change",
					  "params": {
					    "description": "베이지 린넨 + 자연광",
					    "referenceImageUrls": ["https://.../r1.jpg", "https://.../r2.jpg"]
					  }
					}
					"""),
				@ExampleObject(name = "angle_change (좌표)", value = """
					{
					  "jobId": "bizimg_...",
					  "baseId": "V1",
					  "mode": "angle_change",
					  "params": { "rotation": 30, "tilt": -10 }
					}
					"""),
				@ExampleObject(name = "text_create", value = """
					{
					  "jobId": "bizimg_...",
					  "mode": "text_create",
					  "params": { "description": "감성적인 카페 진열대 위의 머그컵 컷" }
					}
					""")
			})
		)
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "edit 생성 (editId 반환)"),
		@ApiResponse(responseCode = "400", description = "mode/params 검증 실패, 슬롯당 4장 초과 등"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "jobId / baseId 없음"),
		@ApiResponse(responseCode = "409", description = "base 결과 이미지 미완성")
	})
	public ResponseEntity<SuccessResponse<EditCreatedResponse>> edit(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody EditImageRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, editService.create(userId, request)));
	}

	@PostMapping("/inpaint")
	@Operation(
		summary = "인페인팅 (OpenAI gpt-image-1 동기 호출)",
		description = """
			마스크 PNG 와 prompt 로 부분 편집을 적용합니다.

			**워크플로우 (worker):**
			1. base 이미지 + mask PNG 를 OpenAI `images/edits` 로 전송
			2. base64 응답 → S3 업로드 → editId 반환

			- 마스크: data URL 또는 순수 base64 (PNG)
            - 알파 채널 0 인 영역만 편집
            - 최대 4MB
			- `size` 는 "auto" / "1024x1024" / "1024x1536" / "1536x1024"
			""",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(value = """
				{
				  "jobId": "bizimg_...",
				  "baseId": "V1",
				  "prompt": "코끼리를 낙타로 바꿔줘",
				  "maskBase64": "data:image/png;base64,iVBORw0K...",
				  "maskWidth": 1024,
				  "maskHeight": 1536,
				  "size": "auto"
				}
				"""))
		)
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "인페인팅 잡 생성 (editId 반환)"),
		@ApiResponse(responseCode = "400", description = "prompt 길이 / 마스크 디코드 실패 / 4MB 초과 등"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "jobId / baseId 없음"),
		@ApiResponse(responseCode = "409", description = "base 결과 이미지 미완성")
	})
	public ResponseEntity<SuccessResponse<EditCreatedResponse>> inpaint(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody InpaintRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, inpaintService.create(userId, request)));
	}
}
