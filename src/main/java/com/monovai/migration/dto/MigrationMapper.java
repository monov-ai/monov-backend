package com.monovai.migration.dto;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MigrationMapper {

	public TemplateMigrationDto toTemplateDto(Map<String, Object> data) {
		return TemplateMigrationDto.builder()
			.id((String) data.get("id"))
			.title((String) data.get("title"))
			.shortDescription((String) data.get("shortDescription"))
			.prompt((String) data.get("prompt"))
			.promptGuide((String) data.get("promptGuide"))
			.category((String) data.get("category"))
			.imagePath((String) data.get("imagePath"))
			.imageUrl((String) data.get("imageUrl"))
			.mediaType((String) data.get("mediaType"))
			.model((String) data.get("model"))
			.credit(data.get("credit") != null ? ((Number) data.get("credit")).intValue() : 0)
			.hashtags((List<String>) data.getOrDefault("hashtags", List.of()))
			.createdAt(data.get("createdAt") != null ? ((Number) data.get("createdAt")).longValue() : null)
			.updatedAt(data.get("updatedAt") != null ? ((Number) data.get("updatedAt")).longValue() : null)
			.build();
	}

	public UserMigrationDto toUserDto(Map<String, Object> data) {
		UserMigrationDto dto = new UserMigrationDto();

		dto.setUid((String) data.get("id"));
		dto.setEmail((String) data.get("email"));
		dto.setName((String) data.get("nickname"));

		return dto;
	}
}
