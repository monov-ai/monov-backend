package com.monovai.domain.content.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsTemplate extends BaseTimeEntity {

	@Id
	@Column(length = 50)
	private String id;

	@Column(length = 200)
	private String name;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private Object data;

	@Builder(access = AccessLevel.PRIVATE)
	private NewsTemplate(String id, String name, Object data) {
		this.id = id;
		this.name = name;
		this.data = data;
	}

	public static NewsTemplate create(String id, String name, Object data) {
		return NewsTemplate.builder().id(id).name(name).data(data).build();
	}

	public void update(String name, Object data) {
		if (name != null) this.name = name;
		if (data != null) this.data = data;
	}
}
