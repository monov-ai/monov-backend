package com.monovai.domain.credit.dto.response;

import com.monovai.domain.credit.entity.UsageWallet;

public record CreditBalanceResponse(
	int credits,
	Usage usage
) {
	public record Usage(int adImage, int aiVideo, int imageToVideo, int legacyCredits) {
	}

	public static CreditBalanceResponse of(UsageWallet w) {
		if (w == null) {
			return new CreditBalanceResponse(0, new Usage(0, 0, 0, 0));
		}
		return new CreditBalanceResponse(
			w.getLegacyCredits(),
			new Usage(w.getAdImage(), w.getAiVideo(), w.getImageToVideo(), w.getLegacyCredits())
		);
	}
}
