package com.monovai.domain.content.dto;

import java.util.List;

public final class NewsDtos {

	private NewsDtos() {
	}

	public record GenerateTextRequest(
		String category, String purpose, String topic, String tone,
		String targetAudience, Integer cardCount, String template
	) {
	}

	public record Card(String headline, String subtitle, String body, String cta) {
	}

	public record GenerateTextResponse(List<Card> cards, String source, String model) {
	}

	public record GenerateHtmlRequest(String ratio, List<Card> cards, String template) {
	}

	public record HtmlCard(String html) {
	}

	public record GenerateHtmlResponse(List<HtmlCard> cards, String source, String model) {
	}
}
