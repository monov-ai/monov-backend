package com.monovai.domain.business.brand.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monovai.domain.business.brand.dto.request.CreateBrandGuideRequest;
import com.monovai.domain.business.brand.dto.request.UpdateBrandGuideRequest;
import com.monovai.domain.business.brand.dto.response.BrandGuideListResponse;
import com.monovai.domain.business.brand.dto.response.BrandGuideResponse;
import com.monovai.domain.business.brand.dto.response.BrandUploadResponse;
import com.monovai.domain.business.brand.service.BrandGuideService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Business / Brand Guides", description = "브랜드 자산 (로고/팔레트/타이포/무드/에셋/가이드라인) 관리")
@RestController
@RequestMapping("/api/v1/business/brand")
@RequiredArgsConstructor
public class BrandGuideController {

	private final BrandGuideService service;

	@GetMapping
	@Operation(summary = "브랜드 가이드 목록", description = "기본(isDefault) 우선 + createdAt desc 로 정렬되어 반환됩니다.")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패")})
	public ResponseEntity<SuccessResponse<BrandGuideListResponse>> list(
		@AuthenticationPrincipal Long userId
	) {
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_FETCH, service.list(userId)));
	}

	@PostMapping
	@Operation(
		summary = "브랜드 가이드 생성",
		description = "첫 번째 브랜드면 자동으로 isDefault=true. `isDefault: true` 로 만들면 다른 브랜드는 일괄 해제됩니다.",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(value = """
				{ "name": "MONOV 브랜드", "description": "Primary brand", "isDefault": false }
				"""))
		)
	)
	@ApiResponses({@ApiResponse(responseCode = "201", description = "생성 성공"),
		@ApiResponse(responseCode = "400", description = "name 누락")})
	public ResponseEntity<SuccessResponse<BrandGuideResponse>> create(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CreateBrandGuideRequest request
	) {
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_CREATE, service.create(userId, request)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "브랜드 가이드 단건 조회", description = "본인 소유만 접근 가능.")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "id 없음")})
	public ResponseEntity<SuccessResponse<BrandGuideResponse>> get(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "브랜드 가이드 슬러그") @PathVariable("id") String id
	) {
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_FETCH, service.get(userId, id)));
	}

	@PatchMapping("/{id}")
	@Operation(
		summary = "브랜드 가이드 부분 수정",
		description = """
			null 인 필드는 무시 (deep merge 가 아닌 필드 단위 교체).
			보호 필드 (id, userId, createdAt) 는 받지 않음.
			`isDefault: true` 로 setting 시 다른 브랜드는 일괄 해제.
			""",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(value = """
				{
				  "palette": [
				    { "role": "primary", "label": "주요", "hex": "#5620bd" },
				    { "role": "secondary", "label": "보조", "hex": "#f4f0ff" }
				  ]
				}
				"""))
		)
	)
	@ApiResponses({@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "id 없음")})
	public ResponseEntity<SuccessResponse<BrandGuideResponse>> update(
		@AuthenticationPrincipal Long userId,
		@PathVariable("id") String id,
		@RequestBody UpdateBrandGuideRequest request
	) {
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_UPDATE, service.update(userId, id, request)));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "브랜드 가이드 삭제",
		description = "Storage 의 `brand_guides/{uid}/{id}/` 폴더도 함께 비우려 시도. (실패해도 doc 삭제는 진행)")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "id 없음")})
	public ResponseEntity<SuccessResponse<Void>> delete(
		@AuthenticationPrincipal Long userId,
		@PathVariable("id") String id
	) {
		service.delete(userId, id);
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_DELETE));
	}

	@PostMapping(value = "/{id}/upload", consumes = {"multipart/form-data"})
	@Operation(
		summary = "브랜드 자산 업로드 (로고/무드/에셋/폰트)",
		description = """
			- 파일 ≤ 25MB.
			- kind 별 허용 MIME:
			  - `logo`: image/svg+xml, image/png, image/jpeg, image/webp
			  - `mood`: image/png, image/jpeg, image/webp
			  - `asset`: 위 + application/pdf, application/postscript, application/illustrator
			  - `font`: font/* (ttf/otf/woff/woff2). 확장자로도 fallback 검증.
			- 응답: id / url(presigned, 7d) / storagePath. 도큐먼트의 identity.logos / identity.moods / assets 배열에 추가하는 것은 별도 PATCH 호출.
			"""
	)
	@ApiResponses({@ApiResponse(responseCode = "201", description = "업로드 성공"),
		@ApiResponse(responseCode = "400", description = "파일 누락 / 크기 초과 / kind 무효 / MIME 무효"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "id 없음")})
	public ResponseEntity<SuccessResponse<BrandUploadResponse>> upload(
		@AuthenticationPrincipal Long userId,
		@PathVariable("id") String id,
		@Parameter(description = "업로드 파일") @RequestPart("file") MultipartFile file,
		@Parameter(description = "logo | mood | asset | font") @RequestParam("kind") String kind,
		@Parameter(description = "표시명 (없으면 파일명)") @RequestParam(value = "name", required = false) String name
	) {
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_CREATE,
			service.upload(userId, id, file, kind, name)));
	}

	@PostMapping("/{id}/default")
	@Operation(summary = "기본 브랜드로 지정", description = "다른 가이드는 일괄 isDefault=false.")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "지정 성공"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "id 없음")})
	public ResponseEntity<SuccessResponse<BrandGuideResponse>> setDefault(
		@AuthenticationPrincipal Long userId,
		@PathVariable("id") String id
	) {
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_UPDATE,
			service.setAsDefault(userId, id)));
	}
}
