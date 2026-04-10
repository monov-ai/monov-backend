package com.monovai.domain.template.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template {

	@Id
	private String id;

	private String title;

	private String shortDescription;

	private String category;

	private int credit;

	private String mediaType;

	private String model;

	private String imagePath;

	@Column(columnDefinition = "TEXT")
	private String imageUrl;

	@Lob
	private String prompt;

	@Lob
	private String promptGuide;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TemplateHashtag> hashtags = new ArrayList<>();

	@Builder
	public Template(String id, String title, String shortDescription,
		String category, int credit, String mediaType,
		String model, String imagePath, String imageUrl,
		String prompt, String promptGuide,
		LocalDateTime createdAt, LocalDateTime updatedAt) {

		this.id = id;
		this.title = title;
		this.shortDescription = shortDescription;
		this.category = category;
		this.credit = credit;
		this.mediaType = mediaType;
		this.model = model;
		this.imagePath = imagePath;
		this.imageUrl = imageUrl;
		this.prompt = prompt;
		this.promptGuide = promptGuide;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void addHashtag(String tag) {
		TemplateHashtag hashtag = new TemplateHashtag(this, tag);
		this.hashtags.add(hashtag);
	}
}
