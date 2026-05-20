package com.beat.observability.sentry

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryAttributeType
import io.sentry.SentryBaseEvent
import io.sentry.SentryEvent
import io.sentry.SentryLogEvent
import io.sentry.SentryLogEventAttributeValue
import io.sentry.protocol.Request
import io.sentry.protocol.SentryTransaction
import io.sentry.protocol.User
import org.slf4j.MDC

class BeatSentryEventProcessor(
    private val moduleName: String,
) : EventProcessor {

    override fun process(event: SentryEvent, hint: Hint): SentryEvent = event.apply {
        enrichBaseEvent(this)
        scrubBaseEvent(this)
        scrubEventPayload(this)
    }

    override fun process(transaction: SentryTransaction, hint: Hint): SentryTransaction = transaction.apply {
        enrichBaseEvent(this)
        scrubBaseEvent(this)
    }

    override fun process(logEvent: SentryLogEvent): SentryLogEvent = logEvent.apply {
        attributes = scrubLogAttributes(attributes.orEmpty() + mdcContext().toLogAttributes())
        body = scrubString(body).orEmpty()
    }

    private fun enrichBaseEvent(event: SentryBaseEvent) {
        val mdc = mdcContext()

        event.setTag(SERVICE_TAG, SERVICE_NAME)
        event.setTag(MODULE_TAG, moduleName)
        mdc.forEach { (key, value) -> event.setTag(key, value) }
        event.contexts.set(BEAT_CONTEXT, mapOf(SERVICE_TAG to SERVICE_NAME, MODULE_TAG to moduleName) + mdc)

        enrichUser(event, mdc)
    }

    private fun enrichUser(event: SentryBaseEvent, mdc: Map<String, String>) {
        val userId = mdc[BaseMdcLoggingFilter.USER_ID_KEY]
            ?.takeUnless { it == BaseMdcLoggingFilter.DEFAULT_GUEST_USER }
        val clientIp = mdc[BaseMdcLoggingFilter.CLIENT_IP_KEY]

        if (userId == null && clientIp == null) {
            return
        }

        event.user = (event.user ?: User()).apply {
            if (id.isNullOrBlank() && userId != null) {
                id = userId
            }
            if (ipAddress.isNullOrBlank() && clientIp != null) {
                ipAddress = clientIp
            }
        }
    }

    private fun scrubBaseEvent(event: SentryBaseEvent) {
        event.request?.let(::scrubRequest)
        event.extras = event.extras?.let(::scrubMap)
        event.user?.data = event.user?.data?.let(::scrubUserData)
        scrubBreadcrumbs(event)
        scrubContexts(event)
    }

    private fun scrubEventPayload(event: SentryEvent) {
        event.message?.let { message ->
            message.formatted = scrubString(message.formatted)
            message.message = scrubString(message.message)
            message.params = message.params?.map { scrubString(it).orEmpty() }
        }
        event.exceptions?.forEach { exception ->
            exception.value = scrubString(exception.value)
        }
    }

    private fun scrubRequest(request: Request) {
        request.headers = request.headers?.let(::scrubStringMap)
        request.envs = request.envs?.let(::scrubStringMap)
        request.others = request.others?.let(::scrubStringMap)
        request.cookies = null
        request.data = scrubValue(request.data)
    }

    private fun scrubBreadcrumbs(event: SentryBaseEvent) {
        event.breadcrumbs?.forEach { breadcrumb ->
            breadcrumb.message = scrubString(breadcrumb.message)
            breadcrumb.data.replaceAll { key, value -> scrubKeyedValue(key, value) }
        }
    }

    private fun scrubContexts(event: SentryBaseEvent) {
        event.contexts.entrySet().toList().forEach { (key, value) ->
            event.contexts.set(key, scrubKeyedValue(key, value))
        }
    }

    private fun scrubLogAttributes(
        attributes: Map<String, SentryLogEventAttributeValue>,
    ): Map<String, SentryLogEventAttributeValue> = attributes.mapValues { (key, attribute) ->
        if (isSensitiveKey(key)) {
            redactedAttribute()
        } else {
            SentryLogEventAttributeValue(attribute.type, scrubValue(attribute.value))
        }
    }

    private fun scrubUserData(data: Map<String, String>): Map<String, String> =
        scrubStringMap(data)

    private fun scrubMap(map: Map<String, Any?>): Map<String, Any?> = map.mapValues { (key, value) ->
        scrubKeyedValue(key, value)
    }

    private fun scrubStringMap(map: Map<String, String>): Map<String, String> = map.mapValues { (key, value) ->
        scrubKeyedValue(key, value)?.toString().orEmpty()
    }

    private fun scrubKeyedValue(key: String, value: Any?): Any? =
        if (isSensitiveKey(key)) REDACTED else scrubValue(value)

    private fun scrubValue(value: Any?): Any? = when (value) {
        null -> null
        is String -> scrubString(value)
        is Map<*, *> -> value.entries.associate { (key, nestedValue) ->
            val stringKey = key?.toString().orEmpty()
            stringKey to scrubKeyedValue(stringKey, nestedValue)
        }
        is Iterable<*> -> value.map(::scrubValue)
        is Array<*> -> value.map(::scrubValue)
        else -> value
    }

    private fun scrubString(value: String?): String? =
        SentrySensitiveDataPolicy.scrubString(value)

    private fun redactedAttribute(): SentryLogEventAttributeValue =
        SentryLogEventAttributeValue(SentryAttributeType.STRING, REDACTED)

    private fun isSensitiveKey(key: String): Boolean = SentrySensitiveDataPolicy.isSensitiveKey(key)

    private fun mdcContext(): Map<String, String> = MDC_KEYS.mapNotNull { key ->
        MDC.get(key)?.takeIf { it.isNotBlank() }?.let { key to it }
    }.toMap(LinkedHashMap())

    private fun Map<String, String>.toLogAttributes(): Map<String, SentryLogEventAttributeValue> =
        mapValues { (_, value) -> SentryLogEventAttributeValue(SentryAttributeType.STRING, value) }

    companion object {
        private const val SERVICE_NAME = "beat-server"
        private const val SERVICE_TAG = "service"
        private const val MODULE_TAG = "module"
        private const val BEAT_CONTEXT = "beat"
        const val REDACTED = SentrySensitiveDataPolicy.REDACTED

        private val MDC_KEYS = listOf(
            BaseMdcLoggingFilter.TRACE_ID_KEY,
            BaseMdcLoggingFilter.SPAN_ID_KEY,
            BaseMdcLoggingFilter.USER_ID_KEY,
            BaseMdcLoggingFilter.CLIENT_IP_KEY,
            BaseMdcLoggingFilter.REQUEST_INFO_KEY,
            BaseMdcLoggingFilter.ROUTE_PATTERN_KEY,
        )
    }
}
