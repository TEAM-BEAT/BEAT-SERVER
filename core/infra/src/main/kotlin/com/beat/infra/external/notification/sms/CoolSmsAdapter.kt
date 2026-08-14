package com.beat.infra.external.notification.sms

import com.beat.contracts.sms.SmsMessage
import com.beat.contracts.sms.SmsPort
import net.nurigo.java_sdk.api.Message
import net.nurigo.java_sdk.exceptions.CoolsmsException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.HashMap

@Service
class CoolSmsAdapter : SmsPort {
    @field:Value("\${spring.coolsms.api.key}")
    private lateinit var apiKey: String

    @field:Value("\${spring.coolsms.api.secret}")
    private lateinit var apiSecret: String

    @field:Value("\${spring.coolsms.api.number}")
    private lateinit var fromPhoneNumber: String

    override fun sendSms(message: SmsMessage) {
        val cleanedPhoneNumber = message.to.replace("-", "")
        val coolsms = Message(apiKey, apiSecret)
        val params =
            HashMap<String, String>().apply {
                put("to", cleanedPhoneNumber)
                put("from", fromPhoneNumber)
                put("type", "SMS")
                put("text", message.text)
            }

        try {
            coolsms.send(params)
        } catch (exception: CoolsmsException) {
            throw RuntimeException("SMS 전송 실패: ${exception.message}", exception)
        }
    }
}
