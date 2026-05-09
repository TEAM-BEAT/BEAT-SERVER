package com.beat.observability.logging.coroutines

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext

object MdcCoroutineContext {

    fun current(): CoroutineContext = MDCContext()

    suspend fun <T> withCurrent(block: suspend () -> T): T = withContext(current()) {
        block()
    }
}
