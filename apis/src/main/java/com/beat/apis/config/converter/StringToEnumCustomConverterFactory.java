package com.beat.apis.config.converter;

import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class StringToEnumCustomConverterFactory implements ConverterFactory<String, Enum<?>> {

	@Override
	public <T extends Enum<?>> Converter<String, T> getConverter(final Class<T> targetType) {
		return new StringToEnumCustomConverter<>(targetType);
	}

	private record StringToEnumCustomConverter<T extends Enum<?>>(Class<T> targetType) implements Converter<String, T> {

		@Override
		public T convert(String source) {
			return Arrays.stream(targetType.getEnumConstants())
				.filter(enumConstant -> enumConstant.name().equalsIgnoreCase(source.trim()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Invalid value"));
		}
	}
}
