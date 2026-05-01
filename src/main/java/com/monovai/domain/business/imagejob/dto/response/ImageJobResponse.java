package com.monovai.domain.business.imagejob.dto.response;

import java.util.List;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.imagejob.entity.ImageJob;

public record ImageJobResponse(
	boolean ok,
	JobView job,
	List<EditView> edits
) {
	public record JobView(
		Long jobId,
		String status,
		String resultImageUrl,
		String errorMessage
	) {
		public static JobView from(ImageJob entity) {
			return new JobView(
				entity.getId(),
				entity.getStatus().name(),
				entity.getResultImageUrl(),
				entity.getErrorMessage()
			);
		}
	}

	public record EditView(
		Long editId,
		Long baseId,
		String editType,
		String status,
		String resultImageUrl,
		String errorMessage
	) {
		public static EditView from(ImageEdit entity) {
			return new EditView(
				entity.getId(),
				entity.getBaseId(),
				entity.getEditType().name(),
				entity.getStatus().name(),
				entity.getResultImageUrl(),
				entity.getErrorMessage()
			);
		}
	}

	public static ImageJobResponse of(ImageJob job, List<ImageEdit> edits) {
		return new ImageJobResponse(
			true,
			JobView.from(job),
			edits.stream().map(EditView::from).toList()
		);
	}
}