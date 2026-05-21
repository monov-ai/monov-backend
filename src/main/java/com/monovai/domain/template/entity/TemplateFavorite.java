package com.monovai.domain.template.entity;

import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "template_favorites",
	uniqueConstraints = @UniqueConstraint(name = "uk_template_fav_user_template", columnNames = {"user_id", "template_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateFavorite extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "template_id", nullable = false, length = 64)
	private String templateId;

	@Builder(access = AccessLevel.PRIVATE)
	private TemplateFavorite(Long userId, String templateId) {
		this.userId = userId;
		this.templateId = templateId;
	}

	public static TemplateFavorite create(Long userId, String templateId) {
		return TemplateFavorite.builder().userId(userId).templateId(templateId).build();
	}
}
