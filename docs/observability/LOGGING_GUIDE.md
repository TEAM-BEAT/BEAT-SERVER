# BEAT Logging Guide

## 1. Architecture Overview

```text
HTTP Request
    │
    ▼
BaseMdcLoggingFilter (OncePerRequestFilter)
    │  ① populateMdc: traceId, spanId, clientIp, userId(GUEST), requestInfo
    │  ② response.setHeader("X-Trace-Id", traceId)
    ▼
SecurityMdcLoggingFilter → JwtAuthenticationFilter (sets SecurityContextHolder)
    ▼
DispatcherServlet → Interceptor.preHandle
    │  ③ RoutePatternMdcInterceptor: routePattern ← HandlerMapping result
    ▼
Controller → @ControllerAdvice (exception handling)
    │  ExceptionCaptureResolver: stores exception in request attribute
    ▼
Interceptor.afterCompletion (routePattern stays in MDC — filter owns cleanup)
    ▼
BaseMdcLoggingFilter.finally
    │  ④ refreshUserIdInMdc(): re-reads SecurityContextHolder after JWT ran
    │  ⑤ emitAccessLog(): accessLog.error/info → console (NOT Sentry)
    │  ⑥ MDC.clear()
```

### Log Pipeline (prod)

```text
Application code
    │
    └── SLF4J Logger
           │
           ├── com.beat.observability.logging.access  →  JsonConsoleAppender (access log only)
           │
           ├── com.beat  (all business code)          →  JsonConsoleAppender
           │                                          →  SentryAppender (ERROR → Sentry event)
           │
           └── Root  (third-party libs)               →  JsonConsoleAppender
                                                         (NOT Sentry — prevents library ERROR flood)
```

---

## 2. MDC Field Reference

| MDC key       | JSON field    | Set by                      | Example value              |
|---------------|---------------|-----------------------------|----------------------------|
| `traceId`     | `trace_id`    | BaseMdcLoggingFilter        | `4bf92f3577b34da6`         |
| `spanId`      | `span_id`     | BaseMdcLoggingFilter        | `00f067aa0ba902b7`         |
| `userId`      | `user_id`     | BaseMdcLoggingFilter.finally| `42` / `GUEST`             |
| `clientIp`    | `client_ip`   | BaseMdcLoggingFilter        | `123.45.67.89`             |
| `requestInfo` | `request`     | BaseMdcLoggingFilter        | `POST /api/performances`   |
| `routePattern`| `route`       | RoutePatternMdcInterceptor  | `/api/performances/{id}`   |
| `status`      | `http_status` | BaseMdcLoggingFilter.finally| `200`                      |
| `elapsed`     | `elapsed_ms`  | BaseMdcLoggingFilter.finally| `45`                       |

**Key invariants**:
- `userId` is `GUEST` before JWT validation; refreshed in filter's finally
- `routePattern` is `NO_ROUTE` if the interceptor never ran (e.g., 404 for unknown path)
- `status` and `elapsed_ms` are set only at access-log emission time, not during request processing
- MDC is cleared in filter's finally; do **not** call `MDC.remove()` in interceptors or services

---

## 3. Logging Rules for Application Code

### 3.1 Log Level Policy

| Level   | When to use                                                        |
|---------|--------------------------------------------------------------------|
| `ERROR` | Unexpected failure that requires immediate investigation. Goes to Sentry (com.beat package). |
| `WARN`  | Expected failure (bad input, timeout) or degraded operation. Does **not** go to Sentry. |
| `INFO`  | Business-significant event (user registered, payment confirmed).   |
| `DEBUG` | Developer diagnostic detail. Must be guarded with `log.isDebugEnabled()` in hot paths. |
| `TRACE` | Extremely verbose. Never leave in production code.                 |

### 3.2 @ControllerAdvice / GlobalExceptionHandler Rule

**Rule**: `handleException(Exception)` logs at `log.error`. This is the **only** intended Sentry path for unexpected 5xx.

Do **not** add `log.error` in individual service methods for the same exception — it creates duplicate Sentry events.

For expected domain errors (`BadRequestException`, `NotFoundException`, etc.), log at `log.warn` or not at all in the handler — the access log already records the HTTP status.

```java
// ✓ Correct: unexpected server error → Sentry
@ExceptionHandler(Exception.class)
protected ResponseEntity<ErrorResponse> handleException(Exception exception) {
    log.error("Unexpected server error: ", exception);   // → SentryAppender
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)...;
}

// ✓ Correct: domain error → warn only, access log records status
@ExceptionHandler(NotFoundException.class)
protected ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException exception) {
    // No log here — access log records 404 at INFO level automatically
    return ResponseEntity.status(HttpStatus.NOT_FOUND)...;
}
```

### 3.3 Prohibited Patterns

```java
// ✗ Bypasses logging pipeline — caught by SharedBoundaryContractTest
System.out.println("debug: " + value);

// ✗ Bypasses logging pipeline — caught by SharedBoundaryContractTest
exception.printStackTrace();

// ✗ Loses stack trace — always pass Throwable as last argument
log.error("Failed: " + exception.getMessage());   // string concat loses trace
log.error("Failed: {}", exception.getMessage());  // {} substitution also loses trace

// ✓ Correct
log.error("Failed processing booking id={}", bookingId, exception);
```

### 3.4 PII (Personally Identifiable Information) Rules

**Never log raw PII.** PII includes: phone number, email, real name, payment card number, social security number.

```kotlin
// ✗ Never
log.info("User registered: phone={}", member.phoneNumber)

// ✓ Use userId (opaque internal ID) or masked value
log.info("User registered: userId={}", member.id)

// ✓ Mask if the raw value is necessary for debugging
log.debug("SMS sent to: {}", maskPhone(phoneNumber))  // maskPhone: "010-****-5678"
```

`userId` in MDC is the internal Long ID, not any PII. It is safe to expose in logs.

---

## 4. Structured Log Fields (prod LogQL)

All prod logs are JSON. High-value LogQL queries:

```logql
# All 5xx errors in the last 1h
{env="prod", level="ERROR"} | json | http_status >= 500

# Slow requests > 500ms
{env="prod", module="apis"} | json | elapsed_ms > 500

# Trace context lookup
{env="prod"} | json | trace_id = "4bf92f3577b34da6"

# Per-route error rate
sum by (route) (rate({env="prod", level="ERROR"} | json [5m]))

# Specific user's request history
{env="prod"} | json | user_id = "42"

# Ingest rate by module (budget monitoring)
sum by (module) (bytes_rate({env="prod"} [1h]))
```

---

## 5. Domain Event Catalog

Business-significant events that **must** be logged at INFO level. These serve as the primary audit trail.

| Event                        | Logger package              | Message format                                      |
|------------------------------|-----------------------------|-----------------------------------------------------|
| User registered              | `com.beat.apis.member`      | `"Member registered: userId={}, provider={}"`       |
| Social login                 | `com.beat.apis.member`      | `"Social login: userId={}, provider={}"`            |
| Performance created          | `com.beat.apis.performance` | `"Performance created: performanceId={}, userId={}"` |
| Booking confirmed            | `com.beat.apis.booking`     | `"Booking confirmed: bookingId={}, userId={}"`      |
| Booking cancelled            | `com.beat.apis.booking`     | `"Booking cancelled: bookingId={}, userId={}"`      |
| Payment completed            | `com.beat.apis.ticket`      | `"Payment completed: ticketId={}, amount={}"`       |
| JWT refresh                  | `com.beat.gateway.jwt`      | `"Token refreshed: userId={}"`                      |
| Batch job started            | `com.beat.batch`            | `"[BATCH] {} started"`                              |
| Batch job completed          | `com.beat.batch`            | `"[BATCH] {} completed: processed={}, failed={}"`   |

---

## 6. Service Examples

### SocialLoginService

```kotlin
// ✓ Log business outcome, not implementation detail
fun login(request: SocialLoginRequest): TokenResponse {
    val member = findOrRegisterMember(request)
    log.info("Social login: userId={}, provider={}", member.id, request.provider)
    return tokenService.issue(member)
}

// ✗ Do not log raw social tokens or personal profile data
// log.debug("Kakao profile: {}", kakaoProfile)  // may contain email/name
```

### PerformanceService

```kotlin
// ✓ Log resource lifecycle with IDs
fun createPerformance(userId: Long, request: PerformanceRequest): Performance {
    val performance = performanceRepository.save(...)
    log.info("Performance created: performanceId={}, userId={}", performance.id, userId)
    return performance
}

// ✓ Warn on business-rule violations, not error
fun validateMakerOwnership(userId: Long, performanceId: Long) {
    if (!performance.isOwnedBy(userId)) {
        log.warn("Ownership check failed: userId={}, performanceId={}", userId, performanceId)
        throw ForbiddenException(...)
    }
}
```

---

## 7. Batch Module

Batch jobs are identified by `module=batch` in Loki. Because batch runs are scheduled and high-volume, follow these rules:

1. **Job boundary logs** at INFO: start + complete/failed with record counts
2. **Per-record logs** at DEBUG only: `log.isDebugEnabled()` guard required
3. **Error threshold alerts**: log ERROR only when the failed count exceeds an acceptable threshold, not per-record

```kotlin
fun runNotificationBatch() {
    log.info("[BATCH] NotificationBatch started")
    var processed = 0; var failed = 0
    for (notification in pendingNotifications()) {
        try {
            send(notification)
            processed++
        } catch (ex: Exception) {
            failed++
            if (log.isDebugEnabled) log.debug("Notification failed: id={}", notification.id, ex)
        }
    }
    if (failed > 0) {
        log.warn("[BATCH] NotificationBatch completed with errors: processed={}, failed={}", processed, failed)
    } else {
        log.info("[BATCH] NotificationBatch completed: processed={}", processed)
    }
}
```

---

## 8. Emergency Access Log Kill-Switch

If Loki ingest spikes (see `infra/grafana/alerts/loki-ingest-budget.yaml`), disable access log emission without redeployment:

```bash
# Set env var on the running container and restart
BEAT_ACCESS_LOG_ENABLED=false
```

Access log (structured HTTP trace) is suppressed. Business logs and Sentry error reporting continue normally. Re-enable once the flood is identified and mitigated.

---

## 9. Loki Label Policy

Only these labels are allowed on Loki streams. Adding new labels requires architecture review.

| Label     | Source                         | Cardinality |
|-----------|-------------------------------|-------------|
| `env`     | Alloy static label             | ~3 (prod/dev/local) |
| `cluster` | Alloy static label             | ~2 |
| `host`    | Alloy static label             | ~5 |
| `module`  | Extracted from container name  | ~6 (apis/admin/batch/...) |
| `level`   | Extracted from JSON `level` field | 5 (TRACE/DEBUG/INFO/WARN/ERROR) |

**Prohibited as labels**: `userId`, `traceId`, `route`, `http_status`, `elapsed_ms`, `client_ip`

These are high-cardinality and must remain as JSON line fields queryable via `| json`.
