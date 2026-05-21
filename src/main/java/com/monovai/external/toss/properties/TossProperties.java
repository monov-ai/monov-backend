package com.monovai.external.toss.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "toss")
public class TossProperties {
	private String baseUrl = "https://api.tosspayments.com";
	/** 단건 결제용 시크릿 (절대 프론트 노출 금지). */
	private String secretKey;
	/** 정기결제(빌링)용 시크릿. */
	private String billingSecretKey;
}
