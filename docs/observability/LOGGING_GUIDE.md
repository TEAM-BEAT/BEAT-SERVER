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

For expected client/application/domain failures (`DomainException`, non-5xx module-local application exception), log the stable code at `log.warn` or do not log — the access log already records the HTTP status. Do not use the user-facing message as the machine identifier.

```java
// ✓ Correct: unexpected server error → Sentry
@ExceptionHandler(Exception.class)
protected ResponseEntity<ErrorResponse> handleException(Exception exception) {
    log.error("Unexpected server error: ", exception);   // → SentryAppender
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)...;
}

// ✓ Correct: expected application error → stable code only
@ExceptionHandler(ApiApplicationException.class)
protected ResponseEntity<ErrorResponse> handleApplicationException(ApiApplicationException exception) {
    log.warn("Application failure: code={}", exception.getErrorCode().getCode());
    return mapByType(exception.getErrorCode());
}
```

### 3.3 Prohibited Patterns

```java
// ✗ Bypasses logging pipeline — caught by SharedBoundaryContractTest
System.out.println("debug: " + value);

// ✗ Bypasses logging pipeline — caught by SharedBoundaryContractTest
exception.printStackTrace();

// ✗ Unexpected failure를 message만 기록하면 stack trace를 잃음
log.error("Failed: " + exception.getMessage());   // string concat loses trace
log.error("Failed: {}", exception.getMessage());  // {} substitution also loses trace

// ✓ Unexpected technical failure: Throwable을 마지막 argument로 전달
log.error("Failed processing booking id={}", bookingId, exception);
```

Throwable은 **예상하지 못한 기술 실패**를 단 한 번 기록할 때만 포함한다. 예상 가능한 domain/application 실패는 stable code로만 분류하고 Throwable을 반복 기록하지 않는다. provider/DB 예외의 raw message·body·header는 비밀값이나 개인정보를 포함할 수 있으므로 global log field에 복사하지 않는다.

### 3.4 PII (Personally Identifiable Information) Rules

**Never log raw PII.** PII includes: phone number, email, real name, payment card number, social security number.

현재 전수 검사에서 Kakao token response 전체, 예매자명, nickname을 기록하는 기존 log가 발견됐습니다. 이들은 허용 예가 아니라 제거 대상 보안 부채입니다. 해당 값은 삭제하거나 opaque ID/고정 상태값으로 바꾸고, token/PII 문자열이 log argument에 들어오지 않는 회귀 검사를 추가해야 합니다.

```kotlin
// ✗ Never
log.info("User registered: phone={}", member.phoneNumber)

// ✓ Use userId (opaque internal ID) or masked value
log.info("User registered: userId={}", member.id)

// ✓ Mask if the raw value is necessary for debugging
log.debug("SMS sent to: {}", maskPhone(phoneNumber))  // maskPhone: "010-****-5678"
```

MDC `userId`는 전화번호·이메일 같은 raw 식별정보는 아니지만 계정과 결합되는 pseudonymous identifier이다. 운영 correlation에 필요한 로그에만 사용하고, 로그 접근 권한·보존 기간·외부 공유 정책을 적용한다. public response, metric label, 불필요한 business log에는 노출하지 않는다.

**URI/query PII risk**: application `requestInfo`는 `method + requestURI`를, nginx `request`는 현재 `$request`를 기록하므로 query string까지 Loki로 전송합니다. path/query에 PII, token, password를 넣지 않습니다. nginx가 `$request_method $uri` 또는 검증된 redaction으로 바뀌기 전까지 “query parameter가 scrub된다”고 가정하지 않습니다. route-level 집계는 `routePattern`을 사용합니다.

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
        throw ApiApplicationException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER)
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

If Loki ingest spikes, `BEAT_ACCESS_LOG_ENABLED`로 application access log를 끌 수 있습니다. 이 값은 JVM 시작 시 한 번 읽으므로 running container의 shell에서 값을 지정하는 것만으로는 바뀌지 않습니다.

```bash
# inventories/<env>/group_vars/all/main.yml
modules:
  <module>:
    env:
      BEAT_ACCESS_LOG_ENABLED: "false"
```

변경 후 표준 deploy/rollback 경로로 해당 container를 recreate하고 실제 environment와 log 감소를 확인합니다. application access log만 억제되며 nginx access log, business log, Sentry error는 계속 남습니다. 원인 완화 후 같은 절차로 다시 활성화합니다. 따라서 이것은 동적 무배포 kill-switch가 아닙니다.

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
