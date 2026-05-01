package com.monovai.domain.business.recommendation.entity.value;

import java.util.List;

public record RecommendationItem(
	String id,
	String title,
	String description,
	List<String> tags,
	boolean recommended,
	GlobalLock globalLock
) {
}