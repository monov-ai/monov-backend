package com.monovai.infrastructure.s3.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.cloud.aws")
public class S3Properties {

	private final Credentials credentials = new Credentials();
	private final Region region = new Region();

	@Getter
	@Setter
	public static class Credentials {
		private String accessKey;
		private String secretKey;
	}

	@Getter
	@Setter
	public static class Region {
		private String staticRegion;
	}
}
