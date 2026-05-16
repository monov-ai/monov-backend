package com.monovai.external.kie.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "kie")
public class KieProperties {
	private String baseUrl = "https://api.kie.ai";
	private String apiKey;
	private String model = "kling-2.6/image-to-video";
}
