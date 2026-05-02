package com.monovai.external.nanobanana.dto;

/**
 * Nanobanana 호출 결과.
 * URL 이 아닌 S3 key 를 반환 — DB 에 key 만 영속, URL 은 응답 시점에 재서명.
 */
public record NanobananaResult(
	String taskId,
	String s3Key
) {
}
