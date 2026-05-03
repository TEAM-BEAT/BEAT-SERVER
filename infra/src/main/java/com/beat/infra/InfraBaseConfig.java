package com.beat.infra;

/**
 * Marker for top-level infra bootstrap configurations selectable through
 * {@link InfraBaseConfigGroup}.
 *
 * <p>Support configurations imported or scanned by those top-level groups must
 * not implement this marker; only enum-owned entrypoint configs should.
 */
public interface InfraBaseConfig {
}
