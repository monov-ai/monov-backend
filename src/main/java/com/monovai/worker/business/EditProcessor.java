package com.monovai.worker.business;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.enums.EditMode;
import com.monovai.domain.business.edit.entity.enums.EditStatus;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.edit.repository.ImageEditRepository;
import com.monovai.domain.business.prompt.service.PromptCompileService;
import com.monovai.external.nanobanana.dto.NanobananaResult;
import com.monovai.external.nanobanana.service.NanobananaService;
import com.monovai.external.openai.service.OpenAiImageEditService;
import com.monovai.infrastructure.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EditProcessor {

	private static final Duration INPUT_TTL = Duration.ofMinutes(15);

	private final ImageEditRepository editRepository;
	private final NanobananaService nanobananaService;
	private final OpenAiImageEditService openAiImageEditService;
	private final PromptCompileService promptCompileService;
	private final S3Service s3Service;
	private final RestClient http = RestClient.create();

	@Transactional
	public void process(Long editId) {
		ImageEdit edit = editRepository.findById(editId).orElseThrow();
		if (edit.getStatus() != EditStatus.PENDING) {
			log.warn("[EditProcessor] 이미 처리됨, 무시: editId={} status={}", editId, edit.getStatus());
			return;
		}

		try {
			String prompt = promptCompileService.compileEditPrompt(edit);
			edit.markRunning(prompt);

			NanobananaResult result;
			if (edit.getMode() == EditMode.INPAINT) {
				result = runInpaint(edit, prompt);
			} else if (edit.getMode() == EditMode.TEXT_CREATE) {
				result = nanobananaService.generateImage(prompt);
			} else {
				List<String> inputs = collectInputImageUrls(edit);
				result = nanobananaService.generateImage(prompt, inputs.toArray(String[]::new));
			}
			edit.markSucceeded(result.taskId(), result.s3Key());
		} catch (Exception e) {
			log.error("[EditProcessor] 처리 실패: editId={}", editId, e);
			edit.markFailed(e.getMessage());
		}
	}

	private NanobananaResult runInpaint(ImageEdit edit, String prompt) {
		EditParams p = edit.getParams();
		if (p == null || p.maskPath() == null || edit.getBaseS3Key() == null) {
			throw new IllegalStateException("inpaint 에 필요한 base / mask 가 없습니다");
		}
		String baseUrl = s3Service.getPreSignedUrlForDownload(edit.getBaseS3Key(), INPUT_TTL);
		String maskUrl = s3Service.getPreSignedUrlForDownload(p.maskPath(), INPUT_TTL);
		byte[] mask = fetchBytes(maskUrl);
		String userPrompt = (p.prompt() != null && !p.prompt().isBlank()) ? p.prompt() : prompt;
		return openAiImageEditService.inpaint(userPrompt, baseUrl, mask, p.size());
	}

	private byte[] fetchBytes(String url) {
		ResponseEntity<byte[]> res = http.get().uri(URI.create(url)).retrieve().toEntity(byte[].class);
		byte[] bytes = res.getBody();
		if (bytes == null || bytes.length == 0) throw new IllegalStateException("mask fetch 빈 응답");
		return bytes;
	}

	private List<String> collectInputImageUrls(ImageEdit edit) {
		List<String> out = new java.util.ArrayList<>();
		if (edit.getBaseS3Key() != null && !edit.getBaseS3Key().isBlank()) {
			out.add(s3Service.getPreSignedUrlForDownload(edit.getBaseS3Key(), INPUT_TTL));
		}
		EditParams p = edit.getParams();
		if (p != null) {
			List<String> refs = p.collectReferenceUrls();
			for (String r : refs) {
				if (r == null || r.isBlank()) continue;
				if (r.startsWith("http://") || r.startsWith("https://")) {
					out.add(r);
				} else {
					out.add(s3Service.getPreSignedUrlForDownload(r, INPUT_TTL));
				}
			}
		}
		return out;
	}

	@Transactional
	public void markFailed(Long editId, String message) {
		editRepository.findById(editId).ifPresent(e -> e.markFailed(message));
	}
}
