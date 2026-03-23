package com.beat.contracts.storage;

import java.util.Map;

public record CarouselPresignedUrls(
	Map<String, String> carouselPresignedUrls
) {
}
