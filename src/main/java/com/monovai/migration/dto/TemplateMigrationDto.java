package com.monovai.migration.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class TemplateMigrationDto {

	private String id;
	private String title;
	private String shortDescription;
	private String category;
	private int credit;
	private String mediaType;
	private String model;
	private String imagePath;
	private String imageUrl;
	private String prompt;
	private String promptGuide;
	private List<String> hashtags;

	private Long createdAt;
	private Long updatedAt;
}
