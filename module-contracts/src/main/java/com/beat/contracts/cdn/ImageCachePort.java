package com.beat.contracts.cdn;

/**
 * Port for interacting with the image cache (CDN).
 * Placed in module-contracts so both `apis` and `admin` modules can utilize it
 * without cyclical dependencies.
 */
public interface ImageCachePort {

	/**
	 * Pre-warms the CDN cache for the given image storage key.
	 * The adapter is responsible for converting the key into a CDN URL and
	 * fetching common variants. Implementations should be best-effort and
	 * non-blocking; failure must never break the calling transaction.
	 *
	 * @param imageKey storage key (e.g. {@code performance/abc123.jpg}).
	 */
	void preWarm(String imageKey);
}
