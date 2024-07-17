package com.beat.domain.booking.application;
import lombok.RequiredArgsConstructor;
import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class CoolSmsService {

    @Value("${spring.coolsms.api.key}")
    private String apiKey;

    @Value("${spring.coolsms.api.secret}")
    private String apiSecret;

    @Value("${spring.coolsms.api.number}")
    private String fromPhoneNumber;

    public void sendSms(String to, String text) throws CoolsmsException {
        String cleanedPhoneNumber = to.replace("-", ""); // 전화번호에서 '-' 제거
        Message coolsms = new Message(apiKey, apiSecret);

        HashMap<String, String> params = new HashMap<>();
        params.put("to", cleanedPhoneNumber);
        params.put("from", fromPhoneNumber);
        params.put("type", "SMS");
        params.put("text", text);

        coolsms.send(params);
    }
}

