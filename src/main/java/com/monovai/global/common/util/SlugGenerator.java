package com.monovai.global.common.util;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * spec 의 slug 형식 생성: {prefix}_{epochSec}_{random6}
 * 예: bizrec_1700000000_abc123
 */
@Component
public class SlugGenerator {

	public String generate(String prefix) {
		long epochSec = System.currentTimeMillis() / 1000;
		String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
		return prefix + "_" + epochSec + "_" + random;
	}
}
