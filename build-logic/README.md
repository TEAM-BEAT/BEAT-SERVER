# build-logic module guide

`build-logic`은 BEAT 멀티모듈 Gradle 빌드의 **convention plugin 모듈**입니다.
각 애플리케이션/라이브러리 모듈이 직접 Spring, Kotlin, Web, Infra, Sentry, Test 의존성을 반복 선언하지 않도록 공통 빌드 정책을 capability 단위로 제공합니다.

> 핵심 원칙: `build-logic`은 제품 런타임 코드가 아니라 빌드 정책 코드입니다. 모듈은 필요한 capability plugin만 명시적으로 선택합니다.

---

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| `beat.web-app`, `beat.jpa-infra` 같은 god convention을 제거하고 web/security/openapi/feign/infra/jpa/runtime capability를 선택형 plugin으로 분리했다. | 각 모듈의 `build.gradle.kts`만 봐도 필요한 runtime/compile capability가 드러나는 구조를 유지한다. | dependency-analysis hard gate 전환 전 남은 advisory report 분류와 예외 정책 정리. |

---

## 역할

- Java/Kotlin toolchain, JVM target, 테스트 실행 정책을 중앙에서 고정한다.
- Spring Boot executable module과 Spring library module의 기본 plugin 조합을 제공한다.
- Web MVC, Security, OpenAPI, Feign, Prometheus, Actuator HTTP runtime 같은 기능을 선택형 convention으로 분리한다.
- Infra/JPA/external-client build surface를 책임 단위로 분리한다.
- Sentry source context upload와 Sentry SDK version alignment를 module-local convention으로 소유한다.
- 보안상 필요한 transitive dependency override/constraint를 실행 모듈 convention에 모은다.

## 허용 의존성

`build-logic`은 제품 모듈에 의존하지 않습니다. 허용되는 입력은 다음뿐입니다.

- Gradle Kotlin DSL
- Gradle plugin marker artifacts
- `../gradle/libs.versions.toml`
- Gradle/Spring/Kotlin/Sentry plugin APIs

## 금지 규칙

- `apis`, `admin`, `batch`, `domain`, `infra`, `gateway`, `observability`, `module-contracts`, `global-support` 같은 제품 모듈에 의존 금지
- application/domain runtime 코드를 import 금지
- `build.gradle.kts` root `subprojects { ... }`로 vendor 정책을 다시 주입 금지
- 하나의 convention에 Web, Security, OpenAPI, Feign, Persistence를 다시 묶는 god convention 재도입 금지
- 실행 모듈이 직접 starter dependency를 우회 추가하기 전에 목적별 convention 검토 없이 추가 금지
- CI/build 전용 secret(`SENTRY_AUTH_TOKEN`)을 runtime container contract로 문서화하거나 전파 금지

---

## 현재 plugin 구조

```text
build-logic/
  build.gradle.kts                 # plugin marker dependency 조립
  settings.gradle.kts              # root version catalog 재사용
  src/main/kotlin/
    beat.kotlin-base.gradle.kts
    beat.library.gradle.kts
    beat.spring-library.gradle.kts
    beat.spring-boot-app.gradle.kts
    beat.test.gradle.kts

    beat.web-mvc.gradle.kts
    beat.web-security.gradle.kts
    beat.openapi.gradle.kts
    beat.feign-runtime.gradle.kts
    beat.actuator-http-runtime.gradle.kts
    beat.prometheus-runtime.gradle.kts

    beat.infra-library.gradle.kts
    beat.jpa-adapter.gradle.kts
    beat.external-client.gradle.kts

    beat.sentry-source-context.gradle.kts
```

---

## Plugin catalog

### Base plugins

| Plugin | 책임 | 적용 대상 |
| --- | --- | --- |
| `beat.kotlin-base` | Kotlin JVM plugin, Java 25 toolchain, JVM 25 bytecode, compiler option | Kotlin/Java code를 갖는 모든 BEAT module의 기반 |
| `beat.library` | `java-library` + `beat.kotlin-base` | 순수 library module |
| `beat.spring-library` | `beat.library`, `beat.test`, Spring dependency-management, Kotlin Spring plugin | Spring type을 compile surface로 갖는 library module |
| `beat.spring-boot-app` | Spring Boot executable 기본, Log4j2, Lombok, test starter, CVE constraint, `BootRun` working dir | `apis`, `admin`, `batch` 같은 bootable module |
| `beat.test` | 모든 `Test` task에 `useJUnitPlatform()` 적용 | Java/JUnit, Spring Boot test, future Kotest/MockK layer |

### Web / runtime capability plugins

| Plugin | 책임 | 현재 적용 |
| --- | --- | --- |
| `beat.web-mvc` | `spring-boot-starter-web`, validation | `apis`, `admin` |
| `beat.web-security` | `spring-boot-starter-security` | `apis`, `admin` |
| `beat.openapi` | Springdoc OpenAPI UI | `apis`, `admin` |
| `beat.feign-runtime` | Spring Cloud BOM + OpenFeign runtime | `apis`, `admin` |
| `beat.actuator-http-runtime` | Actuator health endpoint용 Web runtime만 `runtimeOnly`로 제공 | `batch` |
| `beat.prometheus-runtime` | Prometheus registry를 runtime에만 제공 | `apis`, `batch` |

### Infra capability plugins

| Plugin | 책임 | 현재 적용 |
| --- | --- | --- |
| `beat.infra-library` | Spring library 기반 + Spring Boot core compile/runtime support | `infra` 하위 convention 기반 |
| `beat.jpa-adapter` | JPA adapter compile surface, Kotlin JPA plugin, Spring Boot persistence | `infra` |
| `beat.external-client` | external client compile surface: Spring Web + OpenFeign annotations/BOM | `infra` |

### Observability build plugin

| Plugin | 책임 | 현재 적용 |
| --- | --- | --- |
| `beat.sentry-source-context` | Sentry Gradle plugin, source context bundle, auto upload toggle, Sentry SDK alignment, dependency-analysis task edge 보정 | production/library modules 전체 |

---

## 모듈별 적용 현황

| Module | Plugins |
| --- | --- |
| `apis` | `beat.spring-boot-app`, `beat.web-mvc`, `beat.web-security`, `beat.openapi`, `beat.feign-runtime`, `beat.sentry-source-context`, `beat.prometheus-runtime` |
| `admin` | `beat.spring-boot-app`, `beat.web-mvc`, `beat.web-security`, `beat.openapi`, `beat.feign-runtime`, `beat.sentry-source-context` |
| `batch` | `beat.spring-boot-app`, `beat.actuator-http-runtime`, `beat.sentry-source-context`, `beat.prometheus-runtime` |
| `domain` | `beat.library`, `beat.test`, `beat.sentry-source-context` |
| `gateway` | `beat.spring-library`, `beat.sentry-source-context` |
| `infra` | `beat.jpa-adapter`, `beat.external-client`, `beat.sentry-source-context` |
| `module-contracts` | `beat.library`, `beat.sentry-source-context` |
| `observability` | `beat.spring-library`, `beat.sentry-source-context` |
| `global-support` | `beat.library`, `beat.sentry-source-context` |

---

## Capability selection rule

새 모듈 또는 기존 모듈에 dependency가 필요할 때는 먼저 아래 순서로 판단합니다.

```text
1. 실행 가능한 Spring Boot app인가?
   -> beat.spring-boot-app

2. Spring bean/library compile surface가 필요한가?
   -> beat.spring-library

3. HTTP API MVC controller가 필요한가?
   -> beat.web-mvc

4. Spring Security filter/config가 필요한가?
   -> beat.web-security

5. Swagger/OpenAPI UI가 필요한가?
   -> beat.openapi

6. Feign client runtime을 실행 모듈이 소유해야 하는가?
   -> beat.feign-runtime

7. 단순 actuator HTTP endpoint runtime만 필요한가?
   -> beat.actuator-http-runtime

8. Prometheus scrape 대상인가?
   -> beat.prometheus-runtime

9. JPA adapter 구현 module인가?
   -> beat.jpa-adapter

10. external client annotation/compile surface가 필요한 infra module인가?
    -> beat.external-client
```

직접 `implementation(libs.spring.boot.starter.*)`를 추가하기 전에 위 convention이 이미 존재하는지 먼저 확인합니다.

---

## Version catalog usage rule

`build-logic`은 root `gradle/libs.versions.toml`을 재사용합니다.

```kotlin
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
implementation(libs.findLibrary("spring-boot-starter-web").get())
implementation(libs.findBundle("boot-app-core").get())
```

이 문자열 기반 lookup은 IDE가 unused catalog alias로 오탐할 수 있습니다. 실제 unused 판단은 아래 CI checker가 기준입니다.

```bash
python3 .github/scripts/check_unused_version_catalog_aliases.py
```

---

## Sentry source context rule

Sentry runtime dependency는 `observability`가 소유합니다.

`build-logic`의 `beat.sentry-source-context`는 runtime dependency가 아니라 build/release concern을 소유합니다.

- `includeSourceContext=true`
- `SENTRY_AUTH_TOKEN`이 있을 때만 source bundle auto upload
- `autoInstallation=false`
- Sentry SDK version alignment
- dependency-analysis와 Sentry generated resource task edge 보정

`SENTRY_AUTH_TOKEN`은 CI/build 전용입니다. Runtime container 환경변수 계약으로 추가하지 않습니다.

---

## CVE constraint ownership

`beat.spring-boot-app`은 실행 모듈 boot jar에 영향을 주는 보안 constraint를 소유합니다.

현재 constraint 대상:

- Tomcat embed artifacts
- Jackson 3 artifacts
- Commons FileUpload
- Netty DNS artifacts
- Bouncy Castle provider

이 constraint는 코드 import 사용 여부가 아니라 **runtime transitive dependency 보안 보정**입니다. 제거하려면 먼저 Trivy/GitHub security report와 Spring Boot managed baseline을 확인합니다.

---

## Guard rails

- `RootRetirementContractTest`
  - root build가 executable/runtime dependency를 다시 소유하지 않는지 검증
  - Sentry vendor wiring이 root `subprojects` block으로 돌아오지 않는지 검증
  - catalog alias residue가 재도입되지 않는지 검증
- `SharedBoundaryContractTest`
  - web/infra convention split 유지
  - `beat.web-app`, `beat.jpa-infra` 같은 wrapper/god convention 재도입 방지
  - module-contracts / gateway / observability dependency boundary 검증
- CI
  - `python3 .github/scripts/check_unused_version_catalog_aliases.py`
  - `./gradlew buildHealth` advisory report
  - `./gradlew check verifyModuleBootJars --parallel --build-cache`

---

## 검증 명령

`build-logic` 또는 convention plugin을 수정했다면 최소 아래를 실행합니다.

```bash
python3 .github/scripts/check_unused_version_catalog_aliases.py
./gradlew :build-logic:check --no-daemon --console=plain
./gradlew :test --tests 'com.beat.RootRetirementContractTest' --tests 'com.beat.SharedBoundaryContractTest' --no-daemon --console=plain
./gradlew check verifyModuleBootJars --parallel --build-cache --no-daemon --console=plain -Dorg.gradle.jvmargs='-Xmx3g -XX:MaxMetaspaceSize=1g'
git diff --check
```

`buildHealth`는 dependency-analysis가 많은 classpath를 분석하므로 로컬 기본 heap이 작으면 아래처럼 실행합니다.

```bash
./gradlew buildHealth --no-daemon --console=plain -Dorg.gradle.jvmargs='-Xmx3g -XX:MaxMetaspaceSize=1g'
```

---

## To-Be direction

```text
build-logic/src/main/kotlin/
  beat.<base>.gradle.kts              # 언어/toolchain/test 기반
  beat.<capability>.gradle.kts        # web/security/openapi/feign/prometheus 등 선택형 기능
  beat.<adapter-family>.gradle.kts    # infra/jpa/external-client 같은 adapter family
  beat.<vendor-build>.gradle.kts      # Sentry source context처럼 build/release concern
```

새 convention은 다음 조건을 만족할 때만 추가합니다.

- 두 개 이상 모듈에서 반복되는 빌드 정책이 있다.
- 단일 모듈이라도 capability 이름이 직접 dependency보다 의도를 더 명확히 한다.
- root build나 제품 모듈에 vendor/build-system 세부사항이 새는 것을 막는다.
- 기존 convention에 넣으면 책임이 넓어져 god convention으로 회귀할 위험이 있다.
