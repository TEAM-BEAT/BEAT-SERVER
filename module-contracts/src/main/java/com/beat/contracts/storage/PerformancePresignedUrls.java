package com.beat.contracts.storage;

import java.util.Map;

public record PerformancePresignedUrls(
	Map<String, Map<String, String>> performanceMakerPresignedUrls
) {
}
