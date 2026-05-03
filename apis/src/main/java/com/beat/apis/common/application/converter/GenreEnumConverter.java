package com.beat.apis.common.application.converter;

import com.beat.apis.home.application.dto.HomeGenreType;
import com.beat.apis.performance.application.dto.GenreType;
import com.beat.domain.performance.domain.Genre;

public final class GenreEnumConverter {

	private GenreEnumConverter() {
	}

	public static Genre toDomain(final GenreType genreType) {
		if (genreType == null) {
			return null;
		}

		return switch (genreType) {
			case BAND -> Genre.BAND;
			case PLAY -> Genre.PLAY;
			case DANCE -> Genre.DANCE;
			case ETC -> Genre.ETC;
		};
	}

	public static Genre toDomain(final HomeGenreType homeGenreType) {
		if (homeGenreType == null) {
			return null;
		}

		return switch (homeGenreType) {
			case BAND -> Genre.BAND;
			case PLAY -> Genre.PLAY;
			case DANCE -> Genre.DANCE;
			case ETC -> Genre.ETC;
		};
	}

	public static GenreType toPerformanceApi(final Genre genre) {
		if (genre == null) {
			return null;
		}

		return switch (genre) {
			case BAND -> GenreType.BAND;
			case PLAY -> GenreType.PLAY;
			case DANCE -> GenreType.DANCE;
			case ETC -> GenreType.ETC;
		};
	}

	public static HomeGenreType toHomeApi(final Genre genre) {
		if (genre == null) {
			return null;
		}

		return switch (genre) {
			case BAND -> HomeGenreType.BAND;
			case PLAY -> HomeGenreType.PLAY;
			case DANCE -> HomeGenreType.DANCE;
			case ETC -> HomeGenreType.ETC;
		};
	}
}
