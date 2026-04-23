# BEAT-SERVER 마이그레이션 기준 문서

이 문서는 BEAT-SERVER의 Java → Kotlin 전환을 시작하기 전에 현재 백엔드 구조와 검증 기준을 고정하기 위한 기준 문서입니다.

루트 `README.md`는 제품/프로젝트 소개 문서로 유지합니다. 이 파일은 #384와 상위 migration 이슈 #350을 위한 백엔드 migration 기준점입니다.

## 현재 백엔드 기준

| 구분 | 현재 기준 |
| --- | --- |
| Spring | Spring Boot `4.0.5` |
| Language / Runtime | Kotlin `2.3.20`, Java toolchain JDK `25`, Java compile release / Kotlin JVM target `25` |
| Build | Gradle wrapper, root project는 실행 모듈이 아니라 공통 검증/조정 역할 |
| 실행 모듈 | `apis`, `admin`, `batch` |
| shared/support 모듈 | `domain`, `gateway`, `infra`, `global-utils`, `module-contracts`, `observability` |
| CI | GitHub Actions PR gate: Gradle baseline 검증, Docker image build, Trivy image scan |
| 배포 검증 | `infra/ansible` 기준 Ansible/SOPS secret-aware GitHub Actions 검증 |
| 품질 도구 | SonarQube / Kover Gradle plugin은 존재하지만, SonarCloud 설정과 hard coverage threshold는 #384 범위가 아님 |

### 빌드 설정 동기화 근거

위 기준값은 아래 Gradle 설정과 대조한 값입니다. 버전이나 toolchain을 변경할 때는 이 표와 빌드 설정을 함께 갱신해야 합니다.

| 기준 | 빌드 설정 source of truth | 현재 값 |
| --- | --- | --- |
| Spring Boot | `gradle/libs.versions.toml`의 `spring-boot` | `4.0.5` |
| Kotlin | `gradle/libs.versions.toml`의 `kotlin` | `2.3.20` |
| Java toolchain | root `build.gradle.kts`와 `build-logic/build.gradle.kts`의 `JavaLanguageVersion.of(25)` | JDK `25` |
| Java bytecode target | root `build.gradle.kts`와 `build-logic/build.gradle.kts`의 `options.release.set(25)` | release `25` |
| Kotlin JVM target | root `build.gradle.kts`와 `build-logic/build.gradle.kts`의 `JvmTarget.JVM_25` | JVM `25` |

확인 명령:

```bash
find . \
  \( -name build.gradle.kts -o -name gradle.properties -o -path './gradle/libs.versions.toml' \) \
  -not -path './.git/*' \
  -print0 \
  | xargs -0 rg -n -C2 'spring|boot|kotlin|toolchain|JvmTarget|options\.release|java\s*\{'

rg -n -C2 'Spring Boot|Kotlin|JDK|JVM target|compile release' MIGRATION.md
```

Jenkins 관련 내용은 과거 운영 이력으로는 의미가 있을 수 있지만, 현재 이 저장소의 기준 CI/CD gate는 GitHub Actions입니다.

## 모듈 지도

### 실행 모듈

| 모듈 | 현재 책임 | 문서 |
| --- | --- | --- |
| `apis` | 사용자 대상 HTTP API 실행 모듈. 사용자 API controller, DTO, Swagger/OpenAPI, API 보안 정책을 소유한다. | [`apis/README.md`](apis/README.md) |
| `admin` | 관리자/백오피스 HTTP API 실행 모듈. 관리자 API 정책, DTO, Swagger/OpenAPI, 운영성 workflow entrypoint를 소유한다. | [`admin/README.md`](admin/README.md) |
| `batch` | scheduler/batch 실행 모듈. scheduler runtime, scheduled job, batch maintenance flow를 소유한다. | [`batch/README.md`](batch/README.md) |

### shared/support 모듈

| 모듈 | 현재 책임 | 문서 |
| --- | --- | --- |
| `domain` | 도메인 모델, 도메인 규칙, repository interface의 최종 소유자. 현재는 migration 중이라 JPA entity / Spring Data repository concern이 아직 남아 있다. | [`domain/README.md`](domain/README.md) |
| `gateway` | 실행 모듈이 사용하는 security/JWT/bootstrap public surface. public/internal surface tightening은 아직 후속 작업이다. | [`gateway/README.md`](gateway/README.md) |
| `infra` | JPA entity / persistence model, Spring Data adapter, QueryDSL/Kotlin JDSL query 구현체, domain repository interface 구현체, 외부 adapter와 기술 bootstrap을 소유한다. | [`infra/README.md`](infra/README.md) |
| `global-utils` | shared-kernel 제약과 저수준 공통 유틸리티를 담당한다. | [`global-utils/README.md`](global-utils/README.md) |
| `module-contracts` | auth, notification, schedule, SMS, storage 등 cross-module port/transfer contract를 담당한다. | [`module-contracts/README.md`](module-contracts/README.md) |
| `observability` | logging, metrics, tracing, actuator 관련 shared observability 설정을 담당한다. | [`observability/README.md`](observability/README.md) |

## domain / infra 목표 경계

Kotlin migration의 최종 방향은 domain과 infra의 책임을 아래처럼 분리하는 것입니다.

| 모듈 | 최종 소유 책임 | 소유하지 않는 것 |
| --- | --- | --- |
| `domain` | 도메인 모델, 값 객체, enum, 도메인 서비스, repository interface | JPA entity, Spring Data repository adapter, QueryDSL/Kotlin JDSL 구현, DB/Redis/외부 API 구현 |
| `infra` | JPA entity / persistence model, Spring Data adapter, QueryDSL/Kotlin JDSL query 구현체, domain repository interface 구현체, 외부 API adapter, 기술 bootstrap | 유스케이스 정책, controller/request/response DTO, 도메인 규칙 자체 |

현재는 migration 중이라 일부 JPA entity와 Spring Data repository concern이 `domain` 안에 남아 있습니다. 이 정리는 #380과 #381에서 다루며, #384는 현재 상태와 목표 경계를 문서로 명확히 구분하는 데 집중합니다.

## #380 domain / persistence separation baseline

Issue #380은 현재 `domain` 모듈에 남아 있는 persistence concern을 **즉시 전면 이동**하는 이슈가 아니라, Kotlin migration 전에 아래 기준을 고정하는 baseline입니다.

- `domain` 안의 JPA entity / mapped superclass / Spring Data repository / QueryDSL projection / QueryDSL APT 설정은 transitional concern이다.
- 최종 소유자는 `infra`이며, 이동은 aggregate/use-case slice별로 수행한다.
- #380에서는 현재 inventory와 guard를 먼저 세우고, concrete slice는 aggregate/use-case 단위로 작게 진행한다. QueryDSL/custom repository 구현 경계 변경은 #381과 slice PR로 넘긴다.
- 새 domain 코드가 persistence concern을 추가하려면 먼저 infra ownership 또는 명시 allowlist review가 필요하다.

### Current persistence inventory in `domain`

| Concern | Current files | Current role | Target owner |
| --- | --- | --- | --- |
| Persistence auditing base | `domain/src/main/java/com/beat/domain/BaseTimeEntity.java` | `@MappedSuperclass`, auditing listener, created/modified timestamps | `infra.persistence.common` 계열 persistence base |
| JPA entity / relation mapping | `Booking`, `Cast`, `Member`, `Performance`, `PerformanceImage`, `Schedule`, `Staff`, `Users` under `domain/src/main/java/com/beat/domain/*/domain`; Promotion mapping moved to `infra.persistence.promotion.entity.PromotionJpaEntity` | DB identity, column/enum mapping, relation/cascade/delete mapping | `infra.persistence.<context>.entity` |
| Spring Data repository adapter | `BookingRepository`, `TicketRepository`, `CastRepository`, `MemberRepository`, `PerformanceImageRepository`, `PerformanceRepository`, `ScheduleRepository`, `StaffRepository`, `UserRepository` under `domain/**/dao`; `PromotionJpaRepository` under `infra/persistence/promotion/repository` now targets `PromotionJpaEntity` | `JpaRepository`, JPQL `@Query`, locking/modifying/transaction annotations, derived query methods | `infra.persistence.<context>.repository` |
| Mixed custom/query contracts | `TicketRepositoryCustom`, `ScheduleRepositoryCustom`, `MemberRepositoryCustom` | 현재는 domain package에 있지만 query/adapter 성격이 섞여 있음 | business contract이면 domain port, query/adapter이면 infra; #381/slice PR에서 판정 |
| QueryDSL projection DTO | `domain/src/main/java/com/beat/domain/schedule/dao/dto/MinPerformanceDateDto.java` | `@QueryProjection` read-model/projection | `infra` query/projection package 또는 실행 모듈 read-model package; #381 coupled |
| QueryDSL/JPA build config | `domain/build.gradle.kts` | JPA compileOnly, QueryDSL APT, generated Q-type source dir | entity/repository 이동 후 infra build ownership로 축소 |
| JPA scan/auditing glue | `infra/src/main/java/com/beat/infra/config/JpaConfig.java`, `infra/src/main/java/com/beat/infra/persistence/InfraPersistenceConfig.java` | `@EnableJpaAuditing`, transitional domain entity/repository scan, single infra persistence marker scan, and narrow infra persistence component scan through infra persistence config | domain scan root는 slice별 replacement infra persistence package가 생길 때마다 축소하고, auditing/bootstrap과 infra persistence config import는 infra 유지 |

### Infra persistence import contract

`InfraPersistenceConfig`는 의도적으로 두 경로에서 import된다.

1. Runtime path: `@EnableInfraBaseConfig(JPA)` -> `JpaConfig` -> `@Import(InfraPersistenceConfig.class)`.
   이 경로는 "JPA group을 켜면 persistence adapter scan도 함께 켜진다"는 runtime safety net이다.
2. IDE static-analysis breadcrumb: JPA를 쓰는 실행 모듈의 `InfraConfig.kt`가
   `@Import(InfraPersistenceConfig::class)`를 직접 가진다. IntelliJ가 `DeferredImportSelector` 체인을
   완전히 추적하지 못해 domain `PromotionRepository` injection을 false-positive로 표시하는 것을 피하기 위한
   정적 경로다.

두 import는 물리적으로 중복처럼 보이지만 의도가 다르다. `JpaConfig` 쪽 import를 지우면 새 JPA 실행 모듈이
breadcrumb를 빠뜨렸을 때 runtime bean 등록이 깨질 수 있고, 실행 모듈 쪽 import를 지우면 runtime은 동작하더라도
IDE autowiring 경고가 재발할 수 있다. `@EnableInfraBaseConfig` meta-annotation에는 `InfraPersistenceConfig`를
직접 넣지 않는다. 그렇게 하면 JPA를 선택하지 않은 미래 모듈에도 persistence scan이 강제로 들어가 opt-in 계약이
깨진다.

### Repository ownership criteria

Repository interface를 `domain`에 남길 수 있는 조건은 모두 충족되어야 합니다.

1. 메서드 언어가 저장소/query 기술이 아니라 비즈니스/도메인 언어다.
2. `JpaRepository`, `Page`, `Pageable`, `Sort`, `LockModeType`, QueryDSL Q-type/projection, JPQL `@Query`, Spring Data annotation을 노출하지 않는다.
3. JPA가 아닌 구현체로도 같은 contract를 구현할 수 있다.
4. Spring Data custom fragment discovery를 위한 adapter hook이 아니라 실제 application/domain contract로 소비된다.
5. `dao` target package가 아니라 `repository` 또는 `port` 같은 technology-neutral package로 이동할 수 있다.

위 조건을 만족하지 않으면 해당 interface는 `infra`의 Spring Data adapter 또는 query/read-model adapter가 소유합니다.

### Canonical slice migration order

1. aggregate/use-case cluster 하나를 선택한다.
2. 필요한 경우 technology-neutral domain/application port를 먼저 식별한다.
3. JPA entity와 Spring Data repository adapter를 `infra.persistence.<context>` 아래로 이동하거나 새로 만든다.
4. 순수 domain model이 실제로 사용될 때만 mapper를 추가한다.
5. executable/application service가 Spring Data repository가 아니라 domain/application port를 보도록 조정한다.
6. replacement persistence package가 존재하고 boot/test가 통과한 뒤에만 해당 slice의 `JpaConfig` repository scan과 infra persistence config import를 확장하거나 domain scan root를 축소한다.
7. 이동 완료한 source는 `SharedBoundaryContractTest` allowlist에서 제거한다.
8. `./gradlew --no-daemon transitionBoundaryTest`와 영향 실행 모듈 test/bootJar를 통과시킨다.

### CQRS and mapper policy

CQRS는 처음부터 거창하게 DB나 repository를 전부 둘로 나누자는 뜻이 아니다. 우선 실행 모듈의 서비스 코드에서 **수정/저장 흐름(command)** 과 **조회/응답 조립 흐름(query)** 을 구분하는 것부터 시작한다. `apis`, `admin`, `batch`는 필요할 때 `service/command`, `service/query`로 나눌 수 있다. 다만 DTO package는 command/query로 쪼개지 않고 각 실행 모듈의 `dto/request`, `dto/response` 또는 `application/dto` 규칙을 유지한다.

Mapper는 아래처럼 단순하게 생각한다.

1. `XxxPersistenceMapper`는 **DB 저장용 JPA entity와 domain model 사이의 번역기**다. 예를 들어 `PromotionPersistenceMapper`는 `PromotionJpaEntity <-> Promotion` 변환만 맡는다.
2. 이 mapper는 domain repository 구현 내부에서만 쓴다. 저장/수정 흐름뿐 아니라 `findById`, `findAll`처럼 domain `Promotion`을 다시 만들어야 하는 단순 조회에도 사용할 수 있다.
3. mapper는 DB를 조회하지 않는다. `getReferenceById`, 존재 검증, lazy relation 처리 같은 일은 `PromotionRepositoryImpl` 같은 repository 구현체가 맡는다.
4. 화면/목록/통계처럼 **조회 결과만 빠르게 만들고 싶은 흐름**은 이 mapper를 재사용하지 않는다. 그런 경우에는 나중에 `infra.persistence.<context>.repository.query` 아래에 query 전용 repository와 read row/projection을 따로 둔다.
5. domain repository에 화면 조회용 요구를 계속 추가하지 않는다. `Page`, `Pageable`, `Sort`, QueryDSL/JDSL projection, API response DTO가 필요해지면 domain repository가 아니라 query/read-model adapter 후보로 본다.
6. MapStruct 같은 mapper library는 기본값이 아니다. 필드가 많고 비슷한 객체 변환이 반복될 때만 검토한다. 단순히 boilerplate를 줄이기 위해 새 dependency를 추가하지 않는다.
7. Promotion slice는 `PromotionJpaEntity`와 immutable Kotlin data class `Promotion`이 분리되었고, `PromotionPersistenceMapper`가 직접 작성한 가벼운 번역기로 `PromotionJpaEntity <-> Promotion` 변환만 담당한다. MapStruct 같은 mapper library는 사용하지 않는다.

Promotion 기준 현재 흐름:

```text
도메인 객체가 필요한 흐름:
  admin/batch command service 또는 아직 read-model split 전의 단순 조회
    -> domain.promotion.repository.PromotionRepository
    -> infra.persistence.promotion.repository.PromotionRepositoryImpl
    -> PromotionPersistenceMapper
    -> PromotionJpaEntity

조회 최적화가 필요해진 흐름:
  apis/admin/batch query service
    -> infra.persistence.promotion.repository.query.PromotionQueryRepository
    -> PromotionReadRow / 실행 모듈 response DTO
    -> PromotionPersistenceMapper 사용 안 함
```

### Kotlin migration order and forbidden rules

Kotlin 전환 순서:

1. JPA/Spring Data/QueryDSL import가 없는 enum, 값 객체, 순수 helper.
2. technology-neutral port와 domain service.
3. slice migration으로 도입된 순수 domain model.
4. infra로 이동한 persistence entity. 단, JPA Kotlin proxy/constructor/lazy loading/QueryDSL generation 검증이 같이 있어야 한다.
5. #381에서 query boundary를 결정한 뒤 QueryDSL/JDSL adapter.

금지:

- 현재 `domain` 아래 JPA entity를 그대로 Kotlin으로 변환하면서 persistence separation을 우회하지 않는다.
- 새 pure-domain package에 `jakarta.persistence.*`, `org.springframework.data.*`, `@QueryProjection`, generated Q-type, persistence DTO를 추가하지 않는다.
- replacement infra persistence package 없이 `JpaConfig`의 domain scan root를 먼저 좁히지 않는다.
- repo-wide entity rewrite, all-entity Kotlin conversion, query layer rewrite, jOOQ 도입, 신규 dependency 추가를 #380 baseline에 섞지 않는다.

Kotlin ID value object factory naming:

- `from(value: Long)` — raw id 값을 required identifier value object로 변환한다.
- `fromNullable(value: Long?)` — nullable FK/id를 optional identifier value object로 변환한다.
- `of(...)` — 여러 값 조합이나 DTO/value assembly에는 계속 사용할 수 있지만, 단일 raw id wrapper의 기본 이름으로는 쓰지 않는다.
- `create(...)` — 새 aggregate/domain object 생성.
- `rehydrate(...)` — persistence state에서 domain/JPA object를 복원.
- `newInstance(...)` — infra-specific VO/factory style로만 유지하고 domain ID value object에는 도입하지 않는다.

### First slice candidate ranking

Issue #380의 첫 concrete slice는 "작아 보이는 entity 하나"가 아니라 실행 모듈에서 실제로 쓰이는 persistence boundary를 함께 움직일 수 있는 단위여야 한다. Staff/Cast/Promotion을 현재 사용처 기준으로 비교하면 아래 순서를 따른다.

| Rank | Candidate | Current coupling | Decision |
| --- | --- | --- | --- |
| 1 | `Promotion` | `PromotionService`가 user-facing promotion read flow에서 이미 사용되고, admin/batch carousel flow가 같은 repository seam을 사용한다. `Promotion`은 pure domain model로 분리되었고 JPA relation/mapping은 `PromotionJpaEntity`가 소유한다. | Promotion slice 완료: Kotlin data class `domain.promotion.domain.Promotion`, `infra.persistence.promotion.entity.PromotionJpaEntity`, handwritten `PromotionPersistenceMapper`, `PromotionJpaRepository`, `PromotionRepositoryImpl` 기준으로 고정한다. |
| 2 | `Cast` + `Staff` pair | 두 타입 모두 `PerformanceManagementService`, `PerformanceService`, `PerformanceModifyService`의 생성/조회/수정 응답 흐름에 같이 등장하고, 둘 다 `Performance` JPA entity를 생성자/관계로 직접 받는다. | performance write/read adapter seam이 생긴 뒤 pair로 이동한다. 한쪽만 먼저 이동하면 DTO mapping과 repository ownership이 비대칭이 된다. |
| 3 | `Staff` only | `Staff.create(..., Performance)`, `StaffRepository extends JpaRepository`, `findIdsByPerformanceId`, performance edit/delete reconciliation이 모두 현재 JPA `Performance` aggregate에 묶여 있다. | 첫 slice로는 defer. 순수 Staff model만 추가하면 현재 서비스가 사용하지 않는 dead code가 되거나, 실행 모듈이 infra persistence entity를 직접 import하게 된다. |

### Promotion pure domain / persistence split

#380 concrete slice에서 Promotion은 repository ownership뿐 아니라 JPA entity와 pure domain model까지 분리되었다. `domain.promotion.domain.Promotion`은 JPA annotation을 갖지 않고 `performanceId`만 보유하며, DB FK column mapping은 `PromotionJpaEntity`가 소유한다. application/admin/batch 서비스는 technology-neutral domain contract만 주입받고 infra persistence type을 직접 보지 않는다.

| Layer | Canonical name | Responsibility |
| --- | --- | --- |
| Domain model | `domain/src/main/kotlin/com/beat/domain/promotion/domain/Promotion.kt` | JPA annotation 없는 immutable Kotlin data class domain model. 내부에서는 nested value class `Id`와 `performance.domain.PerformanceId`로 식별자를 감싸고, Java callers에는 Long getter를 제공한다. |
| Domain contract | `domain/src/main/java/com/beat/domain/promotion/repository/PromotionRepository.java` | `Promotion` 저장/조회/삭제에 필요한 interface. Spring Data/JPA annotation과 type을 노출하지 않는다. |
| Infra JPA entity | `infra/src/main/kotlin/com/beat/infra/persistence/promotion/entity/PromotionJpaEntity.kt` | `promotion` table, `performance_id` FK column, column/enum mapping을 소유한다. `Performance` 객체 연관관계는 두지 않는다. Kotlin JPA plugin의 no-arg/all-open preset을 검증하되, JPA mapped property는 body에 `var ... protected set`으로 선언해 외부 setter를 막는다. |
| Infra Spring Data | `infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionJpaRepository.java` | `JpaRepository<PromotionJpaEntity, Long>`, JPQL delete/carousel query 등 Spring Data adapter 세부사항. |
| Infra mapper | `infra/src/main/java/com/beat/infra/persistence/promotion/mapper/PromotionPersistenceMapper.java` | MapStruct 없이 직접 작성한 `PromotionJpaEntity <-> Promotion` 변환기. DB 조회는 하지 않는다. |
| Infra implementation | `infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionRepositoryImpl.java` | Domain `PromotionRepository`를 구현하고 `PromotionJpaRepository` + mapper에 위임한다. `performanceId`를 그대로 저장하며 `Performance` reference를 만들지 않는다. |


### Future Promotion Kotlin JPA entity experiment slice

현재 baseline은 순수 domain `Promotion.kt`와 Kotlin `PromotionJpaEntity.kt`를 최종 상태로 유지한다. `PromotionJpaEntity` Kotlin 변환은 Kotlin 2.3.20 toolchain baseline 위에서 검증되었고, 아래 기준을 유지한다.

Scope:

- 변환 대상은 `infra/src/main/java/com/beat/infra/persistence/promotion/entity/PromotionJpaEntity.java`에서 `infra/src/main/kotlin/com/beat/infra/persistence/promotion/entity/PromotionJpaEntity.kt`로 옮기는 것 하나로 제한한다. `SharedBoundaryContractTest`는 두 source가 동시에 존재하지 않는지도 확인한다.
- `domain/src/main/kotlin/com/beat/domain/promotion/domain/Promotion.kt`, `PromotionRepository`, `PromotionPersistenceMapper`, `PromotionRepositoryImpl`, 실행 모듈 service/API 흐름은 동작 보존 목적의 최소 컴파일 수정 외에는 변경하지 않는다.
- 새 dependency는 추가하지 않는다. Kotlin JPA/noarg/all-open plugin을 되돌려야 한다면 먼저 Java entity 유지 baseline에서 필요한 이유와 컴파일/런타임 증거를 남긴다.
- entity name, table/column mapping, nullable `performance_id`, `CarouselNumber` enum string mapping, Kotlin JPA plugin이 제공하는 no-arg/all-open semantics, `rehydrate(...)` factory contract를 보존한다.
- entity constructor parameter는 `val`/`var` 없는 plain parameter로 받고, JPA mapped property는 body에서 `var ... protected set`으로 선언한다. 이렇게 해야 생성 경로는 `private constructor`/factory로 제한하면서도 외부 임의 setter 호출을 막을 수 있다.
- QueryDSL/JDSL 경계, repository scan, autowiring warning cleanup, 다른 aggregate/entity Kotlin 변환은 이 실험 범위에 포함하지 않는다.

Acceptance criteria:

1. Java baseline을 먼저 확인한다: Java `PromotionJpaEntity` 상태에서 promotion mapper/repository tests와 실행 모듈 context boot tests가 통과해야 한다.
2. Kotlin 변환 후 Java callers가 기존 static factory와 getter contract를 그대로 사용할 수 있어야 한다. `PromotionPersistenceMapperTest`와 compile task로 `PromotionJpaEntity.rehydrate(...)`, `getId()`, `getPromotionPhoto()`, `getPerformanceId()`, `getRedirectUrl()`, `isExternal()`, `getCarouselNumber()` 접근을 검증한다. 필요하면 `@JvmStatic`, Java-visible accessor naming, constructor visibility를 명시적으로 보존한다.
3. JPA가 reflection으로 entity를 생성할 수 있어야 한다. `kotlin("plugin.jpa")`의 no-arg/all-open preset이 깨지면 Java entity rollback을 검토한다.
4. `PromotionPersistenceMapper` 왕복 변환과 `PromotionRepositoryImpl` 저장/조회/삭제 흐름이 DB schema 변경 없이 동일하게 동작해야 한다.
5. `apis`, `admin`, `batch` Spring context가 `PromotionRepository` bean을 하나만 보고, 실행 모듈이 infra persistence entity를 직접 import하지 않아야 한다.
6. 변경 diff는 Promotion JPA entity 변환과 그에 필요한 직접 컴파일 수정, 테스트/문서 갱신으로 제한한다.

Verification commands for the experiment PR:

```bash
git diff --check
```

```bash
./gradlew --no-daemon :infra:compileKotlin :infra:compileJava :domain:compileKotlin :domain:compileJava
```

```bash
./gradlew --no-daemon transitionBoundaryTest
```

```bash
./gradlew --no-daemon :infra:test --tests com.beat.infra.persistence.promotion.mapper.PromotionPersistenceMapperTest
```

```bash
./gradlew --no-daemon :apis:test :admin:test :batch:test --tests '*ContextLoadsTest' --tests '*ApplicationTests'
```

```bash
./gradlew --no-daemon :apis:bootJar :admin:bootJar :batch:bootJar --parallel --build-cache
```

Rollback rule: 위 명령 중 Kotlin JPA proxy/constructor/visibility 이슈가 발생하거나 Gradle plugin 재도입이 다른 Java entity baseline을 흔들면, 실험 PR은 Kotlin entity 변환을 되돌리고 실패 원인을 문서화한다.

#### Risky first-slice candidates explicitly deferred

| Candidate | Why risky as first slice | Decision |
| --- | --- | --- |
| `Performance` | 중심 aggregate로 `Promotion`, `PerformanceImage`, `Cast`, `Staff`, `Schedule`, `Booking` 흐름과 relation/cascade/read DTO가 함께 얽혀 있다. | 첫 slice로 금지. 먼저 adapter seam과 repository ownership 기준을 세운 뒤 다룬다. |
| `Booking` | 예매/티켓/스케줄/사용자 관계와 예약 상태, count/query, payment-adjacent flow가 얽혀 있고 `TicketRepositoryCustomImpl`은 #381 query boundary와 연결된다. | 첫 slice로 금지. `Schedule`/ticket query boundary 이후 다룬다. |
| `Schedule` | inventory/seat count, pessimistic lock, QueryDSL projection `MinPerformanceDateDto`, custom repository implementation, `JpaConfig` scan 변경과 직접 결합된다. | 첫 slice로 금지. #381 query/JpaConfig scan plan과 함께 다룬다. |

#### ADR: defer the Staff-only split

Decision: Staff-only pure-domain/persistence split을 이번 baseline에서 만들지 않는다.

Reason:
- Staff는 Cast와 동일한 lifecycle(공연 생성 시 bulk save, 공연 상세/수정 조회, 공연 수정 시 add/update/delete reconciliation)을 공유한다.
- 현재 Staff repository method는 모두 Spring Data/JPA storage language이며 technology-neutral domain contract가 아니다.
- Staff entity는 `Performance` JPA relation을 필수로 갖기 때문에 Staff만 infra로 이동하면 `Performance` persistence owner와 `JpaConfig` scan 경계를 같이 흔든다.

Follow-up guard:
- Staff-only `infra.persistence.staff` package나 순수 `domain.staff` port/model을 추가하는 PR은 이 ADR과 boundary test를 함께 갱신해야 한다.
- 안전한 첫 Staff 관련 slice는 `Cast`와 함께 performance adapter seam을 만든 뒤 진행한다.

## root project 계약

root project는 실행 모듈이 아니라 조정/검증 모듈입니다.

- `bootJar`: disabled
- `bootRun`: disabled
- 실제 실행 모듈: `apis`, `admin`, `batch`

root verification task:

- `transitionBoundaryTest`: root transition boundary guard test만 실행
- `verifyV2WebBaseline`: `:apis:test`, `:apis:bootJar`, `transitionBoundaryTest`
- `verifyModuleBootJars`: `apis`, `admin`, `batch` boot jar build

## Kotlin migration 단계 지도

#384는 README/CI migration gate baseline 이슈입니다. 실제 구조 변경이 아니라, 현재 상태와 검증 기준을 문서로 고정하는 작업입니다.

| 이슈 | 후속 작업 범위 |
| --- | --- |
| #378 | shared module ownership/package closeout, observability AOP package closeout, dormant cache ownership 결정 |
| #379 | gateway public/internal surface tightening |
| #380 | domain model / repository interface와 infra persistence model / repository implementation 분리 전략 |
| #381 | infra QueryDSL → Kotlin JDSL 경계, `JpaConfig` scan 결정 |
| #382 | `apis`, `admin`, `batch` 내부 CQRS/package rule 정리 |
| #383 | async boundary와 coroutine 도입 범위 확정 |
| #384 | README/CI migration gate baseline 정리 |

이 문서는 package 이동, query 기술 교체, gateway scan 축소, domain repository interface와 infra persistence implementation 분리, coroutine 도입을 승인하는 문서가 아닙니다. 그런 작업은 위 후속 이슈에서 다룹니다.

## #378 shared module closeout baseline

#378 기준 shared module closeout은 아래 결정을 고정한다.

- `observability` AOP source package는 `com.beat.observability.aop`가 소유한다. `ObservabilityModuleConfig`는 기존 activation semantics를 보존하기 위해 아직 component-scan/import를 넓히지 않는다.
- `infra`의 `RedisCacheConfig` / `InfraBaseConfigGroup.REDIS_CACHE`는 infra-owned dormant shared cache extension point로 유지하고, 실행 모듈은 아직 opt-in하지 않는다.
- `infra` 안의 `com.beat.domain.*` package residue는 두 QueryDSL custom repository implementation만 known deferred exception으로 허용하며 #380/#381에서 처리한다.
- `module-contracts`는 Java source를 유지하고 implementation-free contract module로 guard한다.
- `global-utils`는 `com.beat.global.common.*` package를 즉시 rename하지 않고 framework/layer-neutral shared-kernel guard를 우선한다.

## CI와 로컬 검증 기준

application PR gate는 [`.github/workflows/ci-pr.yml`](.github/workflows/ci-pr.yml)에 정의되어 있습니다.

현재 흐름:

1. PR 대상: `develop`, `main`
2. JDK 25 setup
3. Gradle baseline command 실행
4. module Docker image build
5. Trivy image scan

로컬에서 같은 핵심 검증을 수행하려면 아래 명령을 사용합니다.

```bash
git diff --check
```

```bash
./gradlew tasks --all --console=plain | rg -i 'kover|sonar|verifyV2WebBaseline|verifyModuleBootJars|transitionBoundaryTest'
```

```bash
./gradlew verifyV2WebBaseline :admin:test :batch:test verifyModuleBootJars --parallel --build-cache
```

로컬에서 Docker/Testcontainers provider를 초기화할 수 없어 전체 baseline이 실패할 수 있습니다. 이 경우 compile/bootJar 경로가 통과했는지 확인한 뒤, Docker provider 초기화 실패는 코드 회귀가 아니라 환경 gap으로 기록합니다.

## Sonar / Kover 방향

root build에는 SonarQube와 Kover plugin이 적용되어 있습니다.

확인 명령:

```bash
./gradlew tasks --all --console=plain | rg -i 'kover|sonar'
```

#384 기준 정책:

- SonarCloud organization/project/token/quality gate 설정이 끝나기 전까지 `./gradlew sonar`를 PR 필수 gate로 요구하지 않는다.
- #384에서 strict Kover threshold를 새로 추가하지 않는다. coverage baseline과 threshold 정책이 아직 합의되지 않았기 때문이다.
- Kover report visibility는 추후 작은 후속 작업으로 추가할 수 있다. 단, 먼저 local `koverXmlReport` / `koverLog` 성공을 확인해야 한다.

## 배포 인프라 검증

배포 운영 세부 내용은 [`infra/README.md`](infra/README.md)와 `infra/ansible/` 아래에 둡니다.

secret-aware 검증 workflow:

- [`.github/workflows/ansible-secret-aware-verify.yml`](.github/workflows/ansible-secret-aware-verify.yml)

역할:

- SOPS 기반 inventory 복호화 검증
- resolver를 통한 SSH metadata 추출 검증
- dev/prod 환경별 `ansible-lint`
- infra/ansible 또는 관련 workflow/action/script 변경 시 실행

workflow YAML을 수정했다면 가능하면 아래를 실행합니다.

```bash
actionlint .github/workflows/*.yml
```

`actionlint`가 없다면 최소한 YAML parse를 수행합니다.

```bash
ruby -e 'require "yaml"; ARGV.each { |f| YAML.load_file(f) }' \
  .github/workflows/ci-pr.yml \
  .github/workflows/ansible-secret-aware-verify.yml
```

## 문서 리뷰 체크리스트

README 계열만 변경한 경우:

- root `README.md`는 제품/프로젝트 소개 문서로 남아 있는가?
- migration/backend 기준 내용은 `MIGRATION.md`에 있는가?
- 변경한 module README가 현재 상태 / 목표 상태 / 후속 이슈를 명확히 구분하는가?
- To-Be를 이미 완료된 현재 상태처럼 표현하지 않았는가?
- #378~#383에서 다룰 구조 변경을 #384에서 완료한 것처럼 쓰지 않았는가?
- module README 링크와 이슈 번호가 깨지지 않았는가?
