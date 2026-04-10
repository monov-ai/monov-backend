package com.monovai.migration.service;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.monovai.domain.template.entity.Template;
import com.monovai.domain.template.entity.TemplateHashtag;
import com.monovai.domain.template.repository.TemplateRepository;
import com.monovai.migration.dto.TemplateMigrationDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateMigrationService {

	private final TemplateRepository templateRepository;

	public void migrate(TemplateMigrationDto dto) {

		Template template = Template.builder()
			.id(dto.getId())
			.title(dto.getTitle())
			.shortDescription(dto.getShortDescription())
			.prompt(dto.getPrompt())
			.promptGuide(dto.getPromptGuide())
			.category(dto.getCategory())
			.imagePath(dto.getImagePath())
			.imageUrl(dto.getImageUrl())
			.mediaType(dto.getMediaType())
			.model(dto.getModel())
			.credit(dto.getCredit())
			.createdAt(toLocalDateTime(dto.getCreatedAt()))
			.updatedAt(toLocalDateTime(dto.getUpdatedAt()))
			.build();

		// hashtags
		if (dto.getHashtags() != null) {
			dto.getHashtags().forEach(template::addHashtag);;
		}

		templateRepository.save(template);
	}

	private LocalDateTime toLocalDateTime(Long millis) {
		if (millis == null) return null;
		return Instant.ofEpochMilli(millis)
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime();
	}
}
