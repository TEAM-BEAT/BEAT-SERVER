# BEAT-SERVER 마이그레이션 기준 문서

이 문서는 BEAT-SERVER의 Java → Kotlin 전환을 시작하기 전에 현재 백엔드 구조와 검증 기준을 고정하기 위한 기준 문서입니다.

루트 `README.md`는 제품/프로젝트 소개 문서로 유지합니다. 이 파일은 #384와 상위 migration 이슈 #350을 위한 백엔드 migration 기준점입니다.

## 현재 백엔드 기준

| 구분 | 현재 기준 |
| --- | --- |
| Spring | Spring Boot `4.0.3` |
| Language / Runtime | Kotlin `2.2.20`, Java toolchain JDK `25`, Java compile release / Kotlin JVM target `21` |
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
| Spring Boot | `gradle/libs.versions.toml`의 `spring-boot` | `4.0.3` |
| Kotlin | `gradle/libs.versions.toml`의 `kotlin` | `2.2.20` |
| Java toolchain | root `build.gradle.kts`와 `build-logic/build.gradle.kts`의 `JavaLanguageVersion.of(25)` | JDK `25` |
| Java bytecode target | root `build.gradle.kts`와 `build-logic/build.gradle.kts`의 `options.release.set(21)` | release `21` |
| Kotlin JVM target | root `build.gradle.kts`와 `build-logic/build.gradle.kts`의 `JvmTarget.JVM_21` | JVM `21` |

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
| #378 | shared module ownership/package closeout, dormant cache ownership 결정 |
| #379 | gateway public/internal surface tightening |
| #380 | domain model / repository interface와 infra persistence model / repository implementation 분리 전략 |
| #381 | infra QueryDSL → Kotlin JDSL 경계, `JpaConfig` scan 결정 |
| #382 | `apis`, `admin`, `batch` 내부 CQRS/package rule 정리 |
| #383 | async boundary와 coroutine 도입 범위 확정 |
| #384 | README/CI migration gate baseline 정리 |

이 문서는 package 이동, query 기술 교체, gateway scan 축소, domain repository interface와 infra persistence implementation 분리, coroutine 도입을 승인하는 문서가 아닙니다. 그런 작업은 위 후속 이슈에서 다룹니다.

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
