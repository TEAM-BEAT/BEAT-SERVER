package com.beat.contracts.sms;

public record SmsMessage(
	String to,
	String text
) {
}
