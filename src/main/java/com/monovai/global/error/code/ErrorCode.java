package com.monovai.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ErrorCode {
	/* 400 Bad Request */

	BAD_REQUEST_DATA(HttpStatus.BAD_REQUEST, "E400001", "잘못된 요청입니다"),
	INVALID_FIELD_ERROR(HttpStatus.BAD_REQUEST, "E400002", "요청 필드 값이 유효하지 않습니다."),
	MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "E400003", "필수 요청 파라미터가 누락되었습니다"),
	MISSING_HEADER(HttpStatus.BAD_REQUEST, "E400004", "필수 요청 헤더가 누락되었습니다."),
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "E400005", "요청 값 타입이 올바르지 않습니다"),
	DATA_INTEGRITY_VIOLATION(HttpStatus.BAD_REQUEST, "E400007", "데이터 무결성 제약 조건을 위반했습니다"),
	REFRESH_TOKEN_USER_ID_MISMATCH_ERROR(HttpStatus.BAD_REQUEST, "E400009", "리프레쉬 토큰의 사용자 정보가 일치하지 않습니다"),
	INVALID_REFRESH_TOKEN_ERROR(HttpStatus.BAD_REQUEST, "E400010", "잘못된 리프레쉬 토큰입니다"),
	REFRESH_TOKEN_SIGNATURE_ERROR(HttpStatus.BAD_REQUEST, "E400011", "리프레쉬 토큰의 서명이 잘못되었습니다"),
	UNSUPPORTED_REFRESH_TOKEN_ERROR(HttpStatus.BAD_REQUEST, "E400012", "지원하지 않는 리프레쉬 토큰입니다"),
	REFRESH_TOKEN_EMPTY_ERROR(HttpStatus.BAD_REQUEST, "E400013", "리프레쉬 토큰이 비어있습니다"),
	INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "E400009", "지원하지 않는 이미지 확장자 입니다."),
	INVALID_TOOL_CATEGORY(HttpStatus.BAD_REQUEST, "E400012", "존재하지 않는 카테고리 입니다."),
	INVALID_PAGE_MIN_SIZE(HttpStatus.BAD_REQUEST, "E400013", "페이지는 1 이상이어야 합니다."),
	INVALID_PAGE_MAX_SIZE(HttpStatus.BAD_REQUEST, "E400014", "한 번에 18개 이하만 조회할 수 있습니다."),
	REFREH_TOKEN_EMPTY_ERROR(HttpStatus.BAD_REQUEST, "E400015", "리프레시 토큰이 비었습니다"),
	SOCIAL_TYPE_BAD_REQUEST(HttpStatus.BAD_REQUEST, "E400016", "로그인 요청이 유효하지 않습니다."),
	NOTIFICATION_READ_FORBIDDEN(HttpStatus.BAD_REQUEST, "E400017", "다른 사람의 알림을 읽을 수 없습니다."),
	ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "E400018", "이미 신고한 대상입니다."),
	ALREADY_PROCESSED_REPORT(HttpStatus.BAD_REQUEST, "E400019", "이미 처리된 신고입니다."),
	INVALID_STYLE(HttpStatus.BAD_REQUEST, "E400020", "스타일이 유효하지 않습니다."),
	STUDIO_PRODUCT_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "E400021", "스튜디오 스타일은 제품 이미지가 필요합니다."),
	INVALID_ANGLE(HttpStatus.BAD_REQUEST, "E400022", "angle 값이 유효하지 않습니다."),
	INVALID_LIGHTING(HttpStatus.BAD_REQUEST, "E400023", "lighting 값이 유효하지 않습니다."),
	INVALID_RATIO(HttpStatus.BAD_REQUEST, "E400024", "ratio 값이 유효하지 않습니다."),
	RECOMMENDATION_NOT_READY(HttpStatus.BAD_REQUEST, "E400025", "추천이 아직 준비되지 않았습니다."),
	MISSING_FILE(HttpStatus.BAD_REQUEST, "E400026", "파일이 첨부되지 않았습니다."),
	FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "E400027", "파일 크기가 너무 큽니다 (최대 10MB)."),
	S3_KEY_FORBIDDEN(HttpStatus.FORBIDDEN, "E403004", "해당 파일에 접근 권한이 없습니다."),
	INVALID_EDIT_MODE(HttpStatus.BAD_REQUEST, "E400028", "edit mode 가 유효하지 않습니다."),
	EDIT_MODE_INPAINT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "E400029", "inpaint mode 는 아직 지원되지 않습니다."),
	INVALID_EDIT_PARAMS(HttpStatus.BAD_REQUEST, "E400030", "params 값이 mode 에 맞지 않습니다."),
	TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "E400031", "한 슬롯에는 최대 4장의 이미지만 사용할 수 있어요."),
	INVALID_ROTATION_TILT(HttpStatus.BAD_REQUEST, "E400032", "rotation 은 -180..180, tilt 는 -90..90 사이의 숫자여야 해요."),
	INVALID_MASK(HttpStatus.BAD_REQUEST, "E400033", "마스크 이미지가 올바르지 않습니다."),
	MASK_TOO_LARGE(HttpStatus.BAD_REQUEST, "E400034", "마스크 파일 크기가 4MB 를 초과했습니다."),
	INVALID_INPAINT_PROMPT(HttpStatus.BAD_REQUEST, "E400035", "프롬프트는 1~2000자 사이여야 해요."),
	INVALID_FAVORITE_KIND(HttpStatus.BAD_REQUEST, "E400036", "kind 가 유효하지 않아요."),
	INVALID_MEDIA_TYPE(HttpStatus.BAD_REQUEST, "E400037", "mediaType 이 유효하지 않아요."),
	HISTORY_LIMIT_INVALID(HttpStatus.BAD_REQUEST, "E400038", "limit 은 1~200 사이의 숫자여야 해요."),
	IMAGE_URL_REQUIRED(HttpStatus.BAD_REQUEST, "E400039", "imageUrl 이 필요해요."),
	BRAND_GUIDE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "E400040", "브랜드 이름은 필수예요."),
	BRAND_GUIDE_KIND_INVALID(HttpStatus.BAD_REQUEST, "E400041", "kind 는 logo / mood / asset / font 중 하나여야 해요."),
	BRAND_GUIDE_FILE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "E400042", "지원하지 않는 파일 형식이에요."),
	BRAND_GUIDE_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "E400043", "파일이 25MB 를 초과했어요."),
	BRAND_GUIDE_FILE_MISSING(HttpStatus.BAD_REQUEST, "E400044", "업로드할 파일이 없어요."),
	PROXY_URL_REQUIRED(HttpStatus.BAD_REQUEST, "E400045", "url 이 필요해요."),
	PROXY_URL_INVALID(HttpStatus.BAD_REQUEST, "E400046", "url 이 올바르지 않아요."),
	PROXY_HOST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "E400047", "허용되지 않은 호스트예요."),
	PROXY_HTTPS_REQUIRED(HttpStatus.BAD_REQUEST, "E400048", "HTTPS 만 허용돼요."),
	INVALID_TEXT_CREATE_PROMPT(HttpStatus.BAD_REQUEST, "E400049", "text_create 의 설명은 1~500자 사이여야 해요."),
	INVALID_ONBOARDING(HttpStatus.BAD_REQUEST, "E400050", "온보딩 입력이 유효하지 않아요."),
	INVALID_LOCALE(HttpStatus.BAD_REQUEST, "E400051", "지원하지 않는 언어예요. (ko | en | ja)"),
	CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "E400052", "필수 약관에 동의해야 해요."),
	INSUFFICIENT_CREDIT(HttpStatus.BAD_REQUEST, "E400053", "크레딧이 부족해요."),
	PROMO_CODE_INVALID(HttpStatus.BAD_REQUEST, "E400054", "유효하지 않은 프로모션 코드예요."),
	PROMO_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "E400055", "만료된 프로모션 코드예요."),
	PROMO_CODE_ALREADY_REDEEMED(HttpStatus.BAD_REQUEST, "E400056", "이미 사용된 프로모션 코드예요."),
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "E400057", "결제 금액이 일치하지 않아요."),
	UNKNOWN_PACKAGE(HttpStatus.BAD_REQUEST, "E400058", "알 수 없는 패키지/플랜이에요."),
	TEMPLATE_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "E400059", "템플릿 요청 입력이 유효하지 않아요."),
	SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "E409004", "이미 활성화된 구독이 있어요."),
	SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "E404019", "구독을 찾을 수 없어요."),
	BILLING_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "E404020", "빌링키를 찾을 수 없어요."),
	TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404021", "템플릿을 찾을 수 없어요."),
	PROMO_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404022", "프로모션 코드를 찾을 수 없어요."),
	BRAND_KIT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404023", "브랜드 키트를 찾을 수 없어요."),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "E429001", "요청이 너무 잦아요. 잠시 후 다시 시도해주세요."),
	TOSS_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "E502005", "결제 승인에 실패했어요."),
	TOSS_BILLING_FAILED(HttpStatus.BAD_GATEWAY, "E502006", "정기결제 처리에 실패했어요."),
	AI_CONTENT_FAILED(HttpStatus.BAD_GATEWAY, "E502007", "AI 콘텐츠 생성에 실패했어요."),
	BASE_NOT_READY(HttpStatus.CONFLICT, "E409003", "base 의 결과 이미지가 아직 준비되지 않았습니다."),
	GPT_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "E502001", "추천 결과 형식이 올바르지 않습니다."),
	KIE_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "E502002", "영상 생성 요청에 실패했어요."),
	OPENAI_IMAGE_EDIT_FAILED(HttpStatus.BAD_GATEWAY, "E502003", "이미지 편집 요청에 실패했어요."),
	PROXY_UPSTREAM_FAILED(HttpStatus.BAD_GATEWAY, "E502004", "외부 이미지 가져오기에 실패했어요."),
	PROXY_NOT_IMAGE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "E415001", "이미지 응답이 아니에요."),

	/* 401 */

	AUTHENTICATION_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "E401001", "인가코드가 만료되었습니다"),
	REFRESH_TOKEN_EXPIRED_ERROR(HttpStatus.UNAUTHORIZED, "E401002", "리프레쉬 토큰이 만료되었습니다"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401003", "리소스 접근 권한이 없습니다."),
	EMPTY_OR_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E401004", "토큰이 존재하지 않거나 유효하지 않습니다"),

	/* 403  FORBIDDEN */

	ACCESS_DENIED(HttpStatus.FORBIDDEN, "E403000", "접근 권한이 없습니다."),
	BOARD_FORBIDDEN(HttpStatus.FORBIDDEN, "E403001", "게시판 접근 권한이 없습니다."),
	NO_PERMISSION_TO_DELETE(HttpStatus.FORBIDDEN, "E403002", "삭제 권한이 없습니다."),
	USER_SUSPENDED(HttpStatus.FORBIDDEN, "E403003", "활동 정지된 사용자입니다."),

	/* 404 NOT FOUND */

	DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "E404001", "데이터가 존재하지 않습니다"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E404002", "유저가 존재하지 않습니다"),
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "E404003", "리프레쉬 토큰이 존재하지 않습니다"),
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404004", "댓글이 존재하지 않습니다"),
	TOOL_NOT_FOUND(HttpStatus.NOT_FOUND, "E404005", "툴 존재하지 않습니다"),
	BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "E404006", "게시글이 존재하지 않습니다"),
	SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "E404007", "스크랩이 존재하지 않습니다"),
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E404008", "알림이 존재하지 않습니다"),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404009", "신고가 존재하지 않습니다"),
	SOCIAL_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404010", "소셜 로그인 타입이 존재하지 않습니다"),
	RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E404011", "추천 기록을 찾을 수 없습니다"),
	RECOMMENDATION_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "E404012", "선택한 추천을 찾을 수 없습니다"),
	IMAGE_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "E404013", "이미지 잡을 찾을 수 없습니다"),
	BASE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404014", "베이스 항목을 찾을 수 없습니다"),
	IMAGE_EDIT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404015", "수정 항목을 찾을 수 없습니다"),
	VIDEO_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404016", "영상 결과를 찾을 수 없어요."),
	BRAND_GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404017", "브랜드 가이드를 찾을 수 없어요."),
	TEMPLATE_FAVORITE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "E404018", "즐겨찾기 대상 결과물을 찾을 수 없어요."),


	/* 409 CONFLICT */

	DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "E409001", "닉네임 중복입니다"),
	DUPLICATED_EMAIL(HttpStatus.CONFLICT, "E409002", "이메일 중복입니다"),

	/* 500 INTERNAL SERVER ERROR */

	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500001", "서버 내부에서 오류가 발생했습니다"),
	FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "E500002", "이미지 업로드에 실패했습니다"),
	FILE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "E500003", "이미지를 찾을 수 없습니다"),
	FILE_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "E500004", "이미지 삭제에 실패했습니다"),
	UNKNOWN_REFRESH_TOKEN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500005", "알 수 없는 리프레쉬 토큰 오류가 발생했습니다"),
	SEND_NOTIFICATION_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "E500006", "알림 전송에 실패했습니다");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}



