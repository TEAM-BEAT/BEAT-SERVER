package com.beat.infra.external.storage.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.beat.contracts.storage.ImageObjectMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class S3FileStorageAdapterTest {

	@Mock
	private AmazonS3 amazonS3;

	@Test
	void findImageObjectMetadataReturnsHeadObjectMetadata() {
		S3FileStorageAdapter adapter = adapter();
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("image/png");
		metadata.setContentLength(1024L);
		when(amazonS3.getObjectMetadata("bucket", "dev/carousel/image.png")).thenReturn(metadata);

		ImageObjectMetadata result = adapter.findImageObjectMetadata("dev/carousel/image.png");

		assertEquals("image/png", result.getContentType());
		assertEquals(1024L, result.getContentLength());
	}

	@Test
	void findImageObjectMetadataReturnsNullWhenObjectDoesNotExist() {
		S3FileStorageAdapter adapter = adapter();
		AmazonS3Exception exception = new AmazonS3Exception("not found");
		exception.setStatusCode(404);
		when(amazonS3.getObjectMetadata("bucket", "dev/carousel/missing.png")).thenThrow(exception);

		assertNull(adapter.findImageObjectMetadata("dev/carousel/missing.png"));
	}

	@Test
	void findImageObjectMetadataRejectsOtherEnvironmentsWithoutHeadObject() {
		S3FileStorageAdapter adapter = adapter();

		assertNull(adapter.findImageObjectMetadata("prod/poster/poster.png"));

		verifyNoInteractions(amazonS3);
	}

	private S3FileStorageAdapter adapter() {
		S3FileStorageAdapter adapter = new S3FileStorageAdapter(amazonS3);
		ReflectionTestUtils.setField(adapter, "bucket", "bucket");
		ReflectionTestUtils.setField(adapter, "keyPrefix", "dev");
		return adapter;
	}
}
