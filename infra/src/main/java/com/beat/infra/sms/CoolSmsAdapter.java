package com.beat.infra.sms;

import com.beat.contracts.sms.SmsMessage;
import com.beat.contracts.sms.SmsPort;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoolSmsAdapter implements SmsPort {

	@Value("${spring.coolsms.api.key}")
	private String apiKey;

	@Value("${spring.coolsms.api.secret}")
	private String apiSecret;

	@Value("${spring.coolsms.api.number}")
	private String fromPhoneNumber;

	@Override
	public void sendSms(SmsMessage message) {
		String cleanedPhoneNumber = message.to().replace("-", "");
		Message coolsms = new Message(apiKey, apiSecret);

		HashMap<String, String> params = new HashMap<>();
		params.put("to", cleanedPhoneNumber);
		params.put("from", fromPhoneNumber);
		params.put("type", "SMS");
		params.put("text", message.text());

		try {
			coolsms.send(params);
		} catch (CoolsmsException exception) {
			throw new RuntimeException("SMS 전송 실패: " + exception.getMessage(), exception);
		}
	}
}
