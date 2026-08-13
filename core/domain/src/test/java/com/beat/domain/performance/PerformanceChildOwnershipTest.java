package com.beat.domain.performance;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.beat.domain.performance.model.Cast;
import com.beat.domain.performance.model.PerformanceImage;
import com.beat.domain.performance.model.Staff;
import com.beat.domain.sharedkernel.model.AggregateRoot;

class PerformanceChildOwnershipTest {

	@Test
	void performanceChildrenDoNotExposeIndependentAggregateOwnership() {
		for (Class<?> childType : new Class<?>[] {Cast.class, Staff.class, PerformanceImage.class}) {
			assertFalse(AggregateRoot.class.isAssignableFrom(childType));
			assertFalse(Arrays.stream(childType.getDeclaredFields())
				.anyMatch(field -> field.getName().equals("performanceId")));
		}
	}
}
