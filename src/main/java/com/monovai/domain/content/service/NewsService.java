package com.monovai.domain.content.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.monovai.domain.content.dto.NewsDtos.Card;
import com.monovai.domain.content.dto.NewsDtos.GenerateHtmlRequest;
import com.monovai.domain.content.dto.NewsDtos.GenerateHtmlResponse;
import com.monovai.domain.content.dto.NewsDtos.GenerateTextRequest;
import com.monovai.domain.content.dto.NewsDtos.GenerateTextResponse;
import com.monovai.domain.content.dto.NewsDtos.HtmlCard;
import com.monovai.external.openai.config.OpenAiConfig;
import com.monovai.external.openai.service.OpenAiChatService;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

	private static final String MODEL = OpenAiConfig.NEWS_CHAT_MODEL;

	private final OpenAiChatService openAiChatService;

	public GenerateTextResponse generateText(GenerateTextRequest req) {
		int count = req.cardCount() != null ? Math.max(1, Math.min(req.cardCount(), 10)) : 3;
		String system = ("""
			너는 카드뉴스 기획자다. 정확히 %d개의 카드를 생성한다.
			각 카드는 headline, subtitle, body, cta 를 포함한다 (한국어).
			출력은 { cards: [...], source, model } 형식. source 는 "openai", model 은 "%s".
			""").formatted(count, MODEL);
		String user = "카테고리: " + nz(req.category()) + "\n목적: " + nz(req.purpose())
			+ "\n토픽: " + nz(req.topic()) + "\n톤: " + nz(req.tone())
			+ "\n타겟: " + nz(req.targetAudience()) + "\n템플릿: " + nz(req.template())
			+ "\n카드수: " + count;
		try {
			GenerateTextResponse res = openAiChatService.structured(system, user, GenerateTextResponse.class, MODEL);
			return new GenerateTextResponse(res.cards(), "openai", MODEL);
		} catch (Exception e) {
			log.error("[News] generate-text 실패", e);
			throw new BusinessException(ErrorCode.AI_CONTENT_FAILED);
		}
	}

	public GenerateHtmlResponse generateHtml(GenerateHtmlRequest req) {
		List<Card> cards = req.cards() != null ? req.cards() : List.of();
		List<HtmlCard> htmlCards = new ArrayList<>();
		String ratio = req.ratio() != null ? req.ratio() : "1:1";
		for (Card card : cards) {
			String system = """
				너는 카드뉴스 HTML 디자이너다. 주어진 카드 1장을 인라인 스타일이 포함된 완결형 HTML 조각으로 만든다.
				반응형 + 지정 비율을 고려. 출력은 { html } 형식.
				""";
			String user = "비율: " + ratio + "\n템플릿: " + nz(req.template())
				+ "\nheadline: " + nz(card.headline()) + "\nsubtitle: " + nz(card.subtitle())
				+ "\nbody: " + nz(card.body()) + "\ncta: " + nz(card.cta());
			try {
				HtmlCard html = openAiChatService.structured(system, user, HtmlCard.class, MODEL);
				htmlCards.add(html);
			} catch (Exception e) {
				log.error("[News] generate-html 실패", e);
				throw new BusinessException(ErrorCode.AI_CONTENT_FAILED);
			}
		}
		return new GenerateHtmlResponse(htmlCards, "openai", MODEL);
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
