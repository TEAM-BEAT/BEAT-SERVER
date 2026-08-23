package com.beat.infra.external.notification.sms

import net.nurigo.java_sdk.api.Message
import net.nurigo.java_sdk.exceptions.CoolsmsException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.HashMap

@Service
internal class CoolSmsAdapter {
    @field:Value("\${spring.coolsms.api.key}")
    private lateinit var apiKey: String

    @field:Value("\${spring.coolsms.api.secret}")
    private lateinit var apiSecret: String

    @field:Value("\${spring.coolsms.api.number}")
    private lateinit var fromPhoneNumber: String

    fun send(to: String, text: String) {
        val cleanedPhoneNumber = to.replace("-", "")
        val coolsms = Message(apiKey, apiSecret)
        val params =
            HashMap<String, String>().apply {
                put("to", cleanedPhoneNumber)
                put("from", fromPhoneNumber)
                put("type", "SMS")
                put("text", text)
            }

        try {
            coolsms.send(params)
        } catch (exception: CoolsmsException) {
            throw RuntimeException("SMS 전송 실패: ${exception.message}", exception)
        }
    }
}
