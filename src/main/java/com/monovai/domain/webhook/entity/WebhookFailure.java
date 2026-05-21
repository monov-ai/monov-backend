package com.monovai.domain.webhook.entity;

import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "webhook_failures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookFailure extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 50)
	private String provider;

	@Column(length = 100)
	private String eventType;

	@Lob
	private String payload;

	@Column(length = 500)
	private String reason;

	@Builder(access = AccessLevel.PRIVATE)
	private WebhookFailure(String provider, String eventType, String payload, String reason) {
		this.provider = provider;
		this.eventType = eventType;
		this.payload = payload;
		this.reason = reason;
	}

	public static WebhookFailure of(String provider, String eventType, String payload, String reason) {
		return WebhookFailure.builder()
			.provider(provider).eventType(eventType).payload(payload).reason(reason).build();
	}
}
