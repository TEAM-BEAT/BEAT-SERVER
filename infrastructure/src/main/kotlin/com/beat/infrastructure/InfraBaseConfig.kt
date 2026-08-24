package com.beat.infrastructure

/**
 * Marker for top-level infra bootstrap configurations selectable through
 * [InfraBaseConfigGroup].
 *
 * Support configurations imported or scanned by those top-level groups must
 * not implement this marker; only enum-owned entrypoint configs should.
 */
internal interface InfraBaseConfig
