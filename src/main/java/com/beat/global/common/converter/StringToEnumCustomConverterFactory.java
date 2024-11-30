package com.beat.global.common.converter;

import java.util.Arrays;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class StringToEnumCustomConverterFactory implements ConverterFactory<String, Enum<?>> {

	public <T extends Enum<?>> Converter<String, T> getConverter(final Class<T> targetType) {
		return new StringToEnumCustomConverter<>(targetType);
	}

	private record StringToEnumCustomConverter<T extends Enum<?>>(Class<T> targetType) implements Converter<String, T> {

		@Override
		public T convert(String source) {
			return Arrays.stream(targetType.getEnumConstants())
				.filter(enumConstant -> enumConstant.name().equalsIgnoreCase(source.trim()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Invalid value")); // 이 예외는 스프링이 MethodArgumentTypeMismatchException으로 감싼다.
		}
	}
}
