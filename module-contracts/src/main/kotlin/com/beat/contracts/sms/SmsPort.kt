package com.beat.contracts.sms


fun interface SmsPort {

    fun sendSms(message: SmsMessage)
}
