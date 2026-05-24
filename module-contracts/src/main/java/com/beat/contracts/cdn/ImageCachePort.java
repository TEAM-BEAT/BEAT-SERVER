package com.beat.contracts.cdn;

public interface ImageCachePort {

	// Best-effort, non-blocking. 실패해도 caller 트랜잭션에 전파되지 않아야 한다.
	void preWarm(String imageKey);
}
