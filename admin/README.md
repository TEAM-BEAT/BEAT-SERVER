# admin module

`admin`은 BEAT의 **관리자/백오피스 HTTP executable module**입니다. 사용자 API와 다른 인증·인가 정책, Swagger/OpenAPI, 관리자 전용 request/response DTO, 운영성 유스케이스를 소유합니다.

핵심 계약은 다음과 같습니다.

```text
Controller -> Facade -> ApplicationService(command/query) -> Domain / Port
```

`admin`은 root application에 기대지 않고 자체 classpath로 build/boot/test 되어야 합니다.

---

## 1. 현재 완료 상태

| Area | Current contract |
| --- | --- |
| Bootstrap | `AdminApplication`이 root bootstrap 없이 `GatewayModuleConfig`, `InfraConfig`, `ObservabilityModuleConfig`만 명시 import |
| Package shape | context별 `api/facade/application` 구조 적용: `admin.user`, `admin.promotion` |
| Layering | Controller는 Facade만 호출, Facade는 ApplicationService만 호출 |
| CQRS | application service는 command/query로 분리 |
| DTO boundary | request/response/result DTO는 domain type을 import하지 않음 |
| SuccessCode | API response boundary인 `admin.api.response.AdminSuccessCode`가 소유 |
| ErrorCode | admin use-case 실패는 `admin.application.exception.AdminApplicationErrorCode`가 소유 |
| Persistence | admin은 domain repository contract를 주입받고, JPA entity/mapper/repository implementation을 직접 알지 않음 |
| Scheduler | admin은 scheduler owner가 아니며 scheduling bridge를 소유하지 않음 |

---

## 2. 역할

- 관리자/백오피스 HTTP API의 실행 진입점을 제공합니다.
- 관리자용 보안 정책, CORS, converter, Swagger/OpenAPI 정책을 소유합니다.
- 관리자 request/response DTO와 application flow를 소유합니다.
- 사용자 API(`apis`) 서비스나 batch 서비스는 재사용하지 않습니다.
- 공유가 필요한 외부 기능은 `module-contracts`의 공개 port를 통해 사용합니다.

---

## 3. 허용 의존성

- `module-contracts`
- `domain`
- `infra`
- `global-utils`
- `observability`
- `gateway`의 공개 bootstrap/annotation 경계

## 4. 금지 규칙

- `project(":")` 직접 의존 금지
- `apis`, `batch` 직접 의존 금지
- `infra.external.*`, `infra.*.entity`, `infra.*.repository.impl`, `infra.*.repository.jpa` 직접 import 금지
- `gateway.security.*`, `gateway.filter.*`, `gateway.config.*` 직접 import 금지
- root legacy bootstrap lane import 금지
  - `com.beat.BeatApplication`
  - `com.beat.legacyroot.*`
  - root `SecurityConfig` / `WebConfig`
- `adapter`, `controller`, `port/in` transitional package 재도입 금지
- JPA Entity, QueryDSL Q type, Redis document를 admin API DTO로 직접 노출 금지
- raw Domain model을 Controller/Facade/DTO/Result로 올리는 것 금지

---

## 5. Bootstrap contract

```text
admin/
  src/main/kotlin/com/beat/admin/
    AdminApplication.kt
    config/
      InfraConfig.kt

  src/main/java/com/beat/admin/
    config/
    handler/
    swagger/
    user/
    promotion/
```

`AdminApplication`은 다음만 import합니다.

```kotlin
@Import(
    GatewayModuleConfig::class,
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
```

규칙:

- app-level broad `@ComponentScan`은 사용하지 않습니다.
- `AdminSecurityConfig`가 관리자 route whitelist와 인증 정책을 소유합니다.
- admin Swagger/OpenAPI는 non-prod에서만 노출합니다.
- `GatewayModuleConfig`와 `gateway.annotation.CurrentMember`는 공개 경계로 허용합니다.
- `InfraConfig`는 필요한 infra base config group만 명시합니다.
- `beat.scheduler.owner=false`를 유지합니다.

---

## 6. To-Be package structure

현재 admin은 context별 구조를 사용합니다.

```text
com.beat.admin.<context>/
  api/
  facade/
  application/
    service/
      command/
      query/
    dto/
      request/
      response/
      result/      # facade 조합용 내부 결과가 필요할 때만
```

현재 context:

```text
com.beat.admin.user
com.beat.admin.promotion
```

### user context

```text
com.beat.admin.user/
  api/
    AdminUserApi
    AdminUserController
  facade/
    AdminUserFacade
  application/
    service/query/AdminUserQueryService
    dto/response/UserFindAllResponse
```

### promotion context

```text
com.beat.admin.promotion/
  api/
    AdminPromotionApi
    AdminPromotionController
  facade/
    AdminPromotionFacade
  application/
    service/command/AdminPromotionCommandService
    service/query/AdminPromotionQueryService
    dto/request/*
    dto/response/*
    dto/result/AdminPromotionResults
```

현재 presigned-url endpoint는 carousel/banner promotion asset 관리 흐름에 속하므로 `promotion` context가 소유합니다. 향후 관리자 전역 파일 관리 유스케이스가 커질 때만 별도 `admin.file` context를 검토합니다.

---

## 7. Layer boundary standard

```text
API Controller -> Facade -> ApplicationService(command/query) -> DomainService/Entity/RepositoryPort/ReadPort
```

### Controller

- admin-facing HTTP entrypoint입니다.
- Facade만 호출합니다.
- ApplicationService, Repository, Domain model을 직접 호출하지 않습니다.
- `SuccessResponse`와 `AdminSuccessCode`로 HTTP response를 조립합니다.

### Facade

- 관리자 API scenario의 공식 진입점입니다.
- 여러 command/query service 결과를 조합할 수 있습니다.
- transaction, repository, domain service를 직접 소유하지 않습니다.
- raw Domain model을 받거나 반환하지 않습니다.

### ApplicationService

- command/query service를 의미합니다.
- 이 계층만 유스케이스 내부에서 Domain model을 조회/변경/정책 판단에 사용할 수 있습니다.
- Domain model은 이 계층 밖으로 반환하지 않습니다.
- transaction boundary는 command/query service가 소유합니다.
- 순수 domain rule은 Entity/VO/DomainService에 위임합니다.

---

## 8. CQRS rule

BEAT admin의 CQRS는 저장소나 DB를 물리적으로 분리하는 것이 아니라 application service를 command/query로 나누는 것부터 시작합니다.

- command service는 상태 변경 흐름과 transaction을 소유합니다.
- query service는 admin-facing 조회와 response 조립을 소유합니다.
- 단순 조회는 domain repository contract를 사용할 수 있습니다.
- 목록/검색/정렬/통계/projection 조회가 커지면 domain repository를 키우지 않고 `module-contracts` read port/read model과 infra query adapter로 분리합니다.
- query service는 JPA Entity, QueryDSL Q type, EntityManager, infra persistence mapper를 직접 사용하지 않습니다.

---

## 9. DTO / Result rule

기본값은 command/query service가 admin 전용 ResponseDTO를 반환하는 것입니다.

- RequestDTO, ResponseDTO, CommandResult, QueryResult는 Domain model을 필드로 담지 않습니다.
- DTO/Result public factory method는 Domain model을 인자로 받지 않습니다.
- Domain model에서 필요한 primitive/value 추출은 ApplicationService 내부 private method나 내부 assembler에서 끝냅니다.
- Result는 기본 계층이 아닙니다. Facade 조합이 필요하거나 같은 service output을 여러 response shape로 재사용할 때만 둡니다.
- Result도 raw Domain model, JPA Entity, infra projection row를 필드로 담지 않습니다.

```text
단일 유스케이스:
Controller -> Facade -> QueryService -> ResponseDTO

복합 관리자 scenario:
Controller -> Facade -> QueryService A -> QueryResult A
                    -> CommandService B -> CommandResult B
                    -> Final ResponseDTO
```

---

## 10. ErrorCode / SuccessCode ownership

### ErrorCode

```text
admin.application.exception.AdminApplicationErrorCode
```

소유 대상:

- repository lookup 실패
- admin use-case input validation
- admin actor/permission 검증
- 관리자 flow 실패

Domain invariant 실패를 새로 만들 때는 domain ErrorCode를 검토합니다. admin application flow 실패를 domain ErrorCode로 표현하지 않습니다.

### SuccessCode

```text
admin.api.response.AdminSuccessCode
```

성공 응답 문구는 API response boundary가 소유합니다. Domain에는 SuccessCode를 추가하지 않습니다.

---

## 11. Guard rails

- `AdminApplicationTest`
  - bootstrap import 집합 고정
  - broad component scan 금지
  - scheduler owner disabled 계약 고정
  - security/swagger/config 정책 확인
- `AdminArchitectureGuardTest`
  - root dependency 재도입 금지
  - root/gateway/infra forbidden import 금지
  - transitional package/file 재도입 금지
  - Facade forbidden dependency 검증
  - DTO domain import 금지
  - raw domain model public return 금지
  - SuccessCode response boundary 위치 고정
- `AdminModuleContextBootTest`
  - module context boot smoke test
  - context controller/facade/service bean 존재 확인
  - scheduler owner/bridge 미소유 확인
- `AdminDtoJsonContractTest`
  - 기존 request enum 문자열 호환성 고정
  - 기존 response JSON field name 유지 검증

---

## 12. Kotlin migration readiness checklist

Kotlin migration 전에 아래를 유지해야 합니다.

- [x] root bootstrap 직접 의존 없음
- [x] context별 package split 적용
- [x] Controller -> Facade -> ApplicationService 구조 적용
- [x] command/query service 분리
- [x] DTO domain import 제거
- [x] Domain model이 Facade/Controller 밖으로 노출되지 않음
- [x] AdminSuccessCode가 API response boundary에 위치
- [x] architecture guard와 JSON compatibility test 존재
- [x] admin compile/test 통과
