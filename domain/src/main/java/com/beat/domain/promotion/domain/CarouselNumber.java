package com.beat.domain.promotion.domain;

public enum CarouselNumber {
	ONE(1),
	TWO(2),
	THREE(3),
	FOUR(4),
	FIVE(5),
	SIX(6),
	SEVEN(7);

	private final int number;

	CarouselNumber(int number) {
		this.number = number;
	}

	public int getNumber() {
		return number;
	}
}