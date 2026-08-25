package com.beat.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.StringUtils

/**
 * Setter-based binding is kept intentionally during the transition baseline. This avoids
 * constructor binding friction while infra config ownership is still moving across modules.
 */
@ConfigurationProperties(prefix = "thread-pool")
internal class ThreadPoolProperties {
    var coreSize: Int = 2
        set(value) {
            field = value.coerceAtLeast(1)
        }

    var maxPoolSize: Int = 4
        set(value) {
            field = value.coerceAtLeast(1)
        }

    var queueCapacity: Int = 50
        set(value) {
            field = value.coerceAtLeast(1)
        }

    var threadNamePrefix: String = "executor-"
        set(value) {
            if (StringUtils.hasText(value)) {
                field = value
            }
        }
}
