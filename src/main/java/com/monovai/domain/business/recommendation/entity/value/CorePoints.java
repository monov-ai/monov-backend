package com.monovai.domain.business.recommendation.entity.value;

import java.util.List;

public record CorePoints(
	String summary,
	List<String> keywords
) {
}
