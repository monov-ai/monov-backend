package com.monovai.domain.template.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "template_hashtags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateHashtag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String tag;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id")
	private Template template;

	public TemplateHashtag(Template template, String tag) {
		this.template = template;
		this.tag = tag;
	}
}
