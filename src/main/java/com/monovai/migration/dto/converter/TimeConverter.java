package com.monovai.migration.dto.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeConverter {

	public static LocalDateTime fromEpoch(Long epochMilli) {
		return epochMilli == null ? null :
			Instant.ofEpochMilli(epochMilli)
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
	}
}
