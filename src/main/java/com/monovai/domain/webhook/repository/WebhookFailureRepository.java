package com.monovai.domain.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.webhook.entity.WebhookFailure;

public interface WebhookFailureRepository extends JpaRepository<WebhookFailure, Long> {
}
