package com.beat.batch;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = BatchApplication.class)
@ActiveProfiles("test")
@Tag("integration")
class BatchModuleContextBootTest {

	@Test
	void contextLoads() {
	}
}
