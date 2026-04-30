# infra module

> 이 문서는 `infra` 모듈의 현재 application-infra/deployment-docs 상태, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `infra`는 구현 기술과 외부 adapter의 집합소이며, 상위 유스케이스를 알면 안 된다.
> Kotlin JPA entity 작성 규약의 canonical source of truth는 루트 [`MIGRATION.md`](../MIGRATION.md)의 `Canonical Kotlin JPA entity rules` 절이다. 이 README는 모듈 역할만 요약하고, 규약 본문은 중복하지 않는다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Application infra bootstrap (`EnableInfraBaseConfig`, JPA, QueryDSL, async, scheduler groups, external clients) and deployment/Ansible docs coexist in this module. QueryDSL/JPA configuration remain current migration surfaces. Dormant `RedisCacheConfig` / `REDIS_CACHE` is retained as an infra-owned future shared cache extension point. | `infra` owns JPA entity / persistence model, Spring Data adapter, QueryDSL/Kotlin JDSL query implementation, and implementation of domain repository interfaces. Deployment docs stay separated from application infra contracts. | QueryDSL/JDSL and `JpaConfig` scan decisions -> #381; shared cache concrete activation policy -> future cache issue; domain persistence split -> #380. |

## 역할

- JPA/QueryDSL/async 등 기술 설정을 소유한다.
- JPA entity / persistence model, Spring Data adapter, QueryDSL/Kotlin JDSL query 구현체를 소유한다.
- `domain` repository interface의 구현체를 제공한다.
- 외부 API client, 파일 저장소, 메시징, 서드파티 adapter를 구현한다.
- 실행 모듈이 필요한 기술 설정만 명시적으로 import할 수 있는 부트스트랩 진입점을 제공한다.

## 허용 의존성

- `domain`
- `global-utils`

## 금지 규칙

- `apis`, `admin`, `batch`, `gateway` 의존 금지
- UseCase, Controller, 앱 전용 DTO 보유 금지
- infra persistence model / adapter 타입을 실행 모듈 API나 domain 계약으로 직접 노출 금지

## As-Is 패키지 구조

```text
infra/
  src/main/java/com/beat/infra/
    EnableInfraBaseConfig.java
    InfraBaseConfig.java
    InfraBaseConfigGroup.java
    InfraBaseConfigImportSelector.java      # DeferredImportSelector — enum → class 매핑
    config/
      AsyncConfig.java                      # AsyncConfigurer, @Import(TaskExecutorConfig)
      TaskExecutorConfig.java               # beatApplicationTaskExecutor 빈 생성
      TaskSchedulerConfig.java              # taskScheduler 빈 생성
      JpaConfig.java
      MysqlCustomDialect.java
      QueryDslConfig.java
      RedisCacheConfig.java
      ThreadPoolProperties.java
    persistence/
      InfraPersistenceConfig.java              # infra.persistence narrow component scan
      InfraPersistenceMarker.java              # entity/repository/component scan root
      promotion/mapper/PromotionPersistenceMapper.java
      promotion/repository/PromotionJpaRepository.java
      promotion/repository/PromotionRepositoryImpl.java
  src/main/kotlin/com/beat/infra/
    InfraModuleConfig.kt
    persistence/
      promotion/entity/PromotionJpaEntity.kt   # Kotlin JPA reference slice; canonical authoring rules live in ../MIGRATION.md

current transitional sources:
  domain/src/main/java/com/beat/domain/**/dao/       # Spring Data repository concern that should move behind infra boundary
  domain/src/main/java/com/beat/domain/**/domain/    # JPA entity / persistence concern that should be split from domain model
  src/main/java/com/beat/global/common/config/**
```

설명:
- `InfraBaseConfigImportSelector`가 `@EnableInfraBaseConfig`의 enum 값을 읽어 해당 `@Configuration` 클래스를 선택적으로 import한다.
- `InfraPersistenceConfig`는 `JpaConfig`가 runtime safety net으로 import하고, JPA를 쓰는 실행 모듈 `InfraConfig.kt`가 IDE static-analysis breadcrumb로 한 번 더 import한다. 두 경로 모두 의도된 중복이며, `@EnableInfraBaseConfig` meta-annotation에 persistence를 직접 넣지는 않는다.
- `AsyncConfig`는 `@Import(TaskExecutorConfig.class)`로 executor 빈만 전이 로드하고, infra는 security-aware wrapper를 직접 소유하지 않는다.
- scheduler bean은 `TaskSchedulerConfig` + `InfraBaseConfigGroup.SCHEDULER`로 분리되어 batch에서만 명시적으로 가져간다.
- Redis runtime wiring은 Spring Boot auto-configuration과 gateway-owned config가 담당하고, infra는 더 이상 gateway-specific Redis bean을 소유하지 않는다.
- future shared caching은 dormant `RedisCacheConfig` + `InfraBaseConfigGroup.REDIS_CACHE`에서 시작하고, 현재 실행 모듈은 아직 이를 import하지 않는다. 활성화 전에는 cache name, TTL, serializer, namespace, invalidation policy, owner module, runtime opt-in이 먼저 정해져야 한다.
- #378 기준 `RedisCacheConfig`는 삭제하지도 활성화하지도 않는 infra-owned dormant extension point다. gateway refresh-token Redis storage와 shared cache bootstrap은 별도 경계다.
- Kotlin JPA entity 작성 규칙은 root [`MIGRATION.md`](../MIGRATION.md)의 canonical guide를 따른다. `PromotionJpaEntity.kt`는 현재 검증된 reference slice이고, rule 본문을 이 README에 중복 복제하지 않는다.
- 일부 공통 config와 Promotion JPA entity / mapper / repository adapter / implementation은 `infra`로 이동했지만, JPA entity / Spring Data repository adapter / query 구현 상당수는 아직 `domain` 쪽 transitional package에 남아 있다.
- 즉 `infra`도 아직 최종형이 아니라 **persistence 구현 책임을 받아오는 이관 진행 중인 landing zone**이다.

## #378 known deferred package exceptions

#415에서 Schedule query/custom repository boundary는 `infra.persistence.schedule.repository.query`로 이동했고,
#418에서 Booking/Ticket repository port도 `domain.booking.repository`로 정리되었다.
따라서 현재 Booking/Schedule 쪽에는 `com.beat.domain.*.dao` 또는 `infra/src/main/java/com/beat/domain/...`
형태의 deferred package exception을 두지 않는다.

`SharedBoundaryContractTest`는 infra source에서 `com.beat.domain.*` package residue가 다시 생기지 않도록 고정한다.

## To-Be 패키지 구조

```text
com.beat.infra.config.*
com.beat.infra.external.<provider>
com.beat.infra.persistence.<context>.entity
com.beat.infra.persistence.<context>.repository
com.beat.infra.persistence.<context>.mapper             # domain repository persistence-domain mapping이 필요할 때만
com.beat.infra.persistence.<context>.repository.query   # read 최적화/query projection이 필요할 때만
```

설명:
- `domain.<context>.repository.XxxRepository` 같은 repository interface는 `domain`이 소유한다.
- 그 interface의 구현체는 `infra.persistence.<context>.repository`가 맡는다.
- JPA entity / persistence model은 `infra.persistence.<context>.entity`가 소유한다.
- Spring Data JPA adapter는 `infra.persistence.<context>.repository`에 두되, domain repository interface가 아니라 infra 내부 구현 세부사항으로 취급한다.
- mapper를 도입한다면 `XxxPersistenceMapper`는 pure domain model과 infra persistence entity가 실제로 분리된 slice에서만 추가한다. 쉽게 말해 DB 저장용 객체와 도메인 객체 사이의 번역기다. repository 조회, lazy reference 획득, query projection 조립은 mapper 책임이 아니다.
- query 전용 구현은 지금 기본값이 아니고, 조회 복잡도 증가나 jOOQ/Kotlin JDSL 도입이 필요할 때만 `repository.query`를 추가한다. 이때 화면/목록/통계용 read model은 persistence mapper를 재사용하지 않고 query 전용 row/DTO로 바로 만든다.

## 최종 목표

- `infra.external.*` 타입을 상위 실행 모듈이 직접 import하지 않는다.
- `InfraModuleConfig`가 실제 기술 import를 모으는 진입점으로 성장한다.
- JPA/QueryDSL/async 부트스트랩이 명시적으로 조립된다.
- JPA entity / Spring Data adapter / query 구현체 / domain repository 구현체는 infra 책임으로 모인다.
- shared cache가 필요해질 때 `REDIS_CACHE` 그룹으로 확장한다.

---

# Deployment Infrastructure

> `infra/ansible/` 디렉토리는 BEAT 서버의 배포 자동화를 담당한다.
> GitHub Actions + Ansible + SOPS(age) 조합으로 dev/prod 환경을 관리한다.

## 전체 배포 아키텍처

```mermaid
flowchart TB
    subgraph GitHub["GitHub"]
        PR["PR → develop/main"]
        PushDevelop["Push → develop"]
        PushProtected["Push → develop/main"]
        ReleasePublished["Release → published"]
        Dispatch["Manual Dispatch"]
    end

    subgraph Workflows["GitHub Actions"]
        CIPR["ci-pr.yml<br/>테스트 + Trivy 스캔"]
        AnsibleLint["ansible-lint.yml<br/>PR-safe lint"]
        SecretAware["ansible-secret-aware-verify.yml<br/>SOPS 활성 검증"]
        DeployDev["deploy-dev.yml<br/>자동 배포"]
        DeployProd["deploy-prod.yml<br/>자동 배포 (release tag)"]
        RollbackProd["rollback-prod.yml<br/>수동 롤백"]
        AnsibleExec["_ansible-exec.yml<br/>공통 실행 엔진"]
    end

    subgraph Tooling["Setup Ansible Tooling"]
        Cosign["Cosign (서명 검증)"]
        Age["age v1.3.1 (SOPS 복호화)"]
        SOPS["SOPS v3.12.2 (시크릿)"]
        Ansible["ansible-core 2.17.14 + ansible-lint"]
        SSH["SSH (fingerprint 검증)"]
    end

    subgraph Server["Target EC2"]
        DevServer["dev-server"]
        ProdServer["prod-server"]
    end

    PR --> CIPR
    PR --> AnsibleLint
    PushDevelop --> DeployDev
    PushProtected --> SecretAware
    ReleasePublished --> DeployProd
    Dispatch --> RollbackProd
    DeployDev --> AnsibleExec
    DeployProd --> AnsibleExec
    RollbackProd --> AnsibleExec
    AnsibleLint --> Tooling
    SecretAware --> Tooling
    AnsibleExec --> Tooling
    AnsibleExec -->|"SSH"| DevServer
    AnsibleExec -->|"SSH"| ProdServer
```

## CI/CD 파이프라인

### Dev 배포 (자동)

```mermaid
flowchart LR
    A["push → develop"] --> B["detect-changes<br/>(paths-filter)"]
    B -->|"apis/** 변경"| C["matrix: apis"]
    B -->|"admin/** 변경"| D["matrix: admin"]
    B -->|"shared/** 변경"| E["matrix: all"]
    C & D & E --> F["verify<br/>(Gradle 테스트)"]
    F --> G["build-image<br/>(Docker 병렬 빌드)"]
    G --> H["foundation<br/>(Ansible 기반 스택 확인)"]
    H --> I["deploy<br/>(Ansible 순차 실행)"]
    I --> J["Slack 알림"]
```

- `shared` 경로(domain, gateway, build-logic, gradle 등) 변경 시 **전체 모듈** 배포
- 이미지 태그: `dev-${GITHUB_SHA}`
- foundation job은 build-image 이후 deploy 전에 실행되며, deploy와 같은 `deploy-dev-runtime-${{ github.ref }}` concurrency group을 사용하고 `needs: foundation`으로 marker 생성을 강제한다
- deploy job은 `max-parallel: 1` — nginx 설정 충돌 방지

### Ansible lint / secret-aware 검증

- `ansible-lint.yml`
  - PR-safe lint 경로
  - `working-directory: infra/ansible`
  - `ANSIBLE_VARS_ENABLED=host_group_vars`로 SOPS vars plugin을 비활성화하고 playbook/role 구조만 검증
- `ansible-secret-aware-verify.yml`
  - push to `develop`/`main`, `workflow_dispatch`에서 실행
  - `environment: dev` / `environment: prod` 각각에서 `AGE_SECRET_KEY`를 읽음
  - dev/prod 각각에서 `apis`, `admin`, `batch` 3개 module group을 모두 resolver로 검증
  - 현재 dev/prod inventory가 같은 host를 가리키더라도, `${module}_servers` contract drift를 조기에 잡기 위해 세 모듈 그룹을 모두 검사
  - `ansible-inventory --list`로 SOPS 복호화가 실제로 되는지 확인
  - 같은 환경에서 `ansible-lint playbooks/*.yml roles`도 실행해 SOPS 활성 상태의 lint까지 검증

### Prod 배포 (자동, release published)

```mermaid
flowchart LR
    A["release.published<br/>(shared tag)"] --> B["resolve-release<br/>tag + 3-module matrix"]
    B --> C["resolve tag once<br/>commit SHA capture"]
    C --> D["verify<br/>checkout by commit SHA"]
    D --> E["build-image<br/>apis/admin/batch<br/>checkout by commit SHA"]
    E --> F["docker push<br/>tag: $release_tag + prod-latest"]
    F --> G["foundation<br/>(checkout_ref = commit SHA)"]
    G --> H["deploy<br/>(Ansible, module sequential,<br/>checkout_ref = commit SHA)"]
    H --> I["Slack 알림"]
```

- `github.event.release.tag_name` 을 **prod 공통 버전 source**로 사용
- `apis/admin/batch` 3개 모듈을 **같은 release tag**로 build/push/deploy
- release tag는 `resolve-release` 단계에서 한 번만 해석하고, 이후 verify/build/deploy는 모두 **immutable commit SHA** 를 사용해 TOCTOU를 방지한다
- 이미지 태그는 모듈별로 `:{release_tag}` 와 `:prod-latest` 를 함께 push
- deploy 단계는 `max-parallel: 1` 로 모듈을 순차 배포
- `release-drafter.yml` 은 draft release 갱신용이며, **실제 prod deploy trigger는 published release** 뿐이다
- `rollback-prod.yml` 은 계속 **수동 workflow_dispatch** 로 유지
- foundation/deploy/rollback은 모두 `concurrency: prod-runtime` — prod 런타임 변경은 동시에 1개만
- resolver는 `ansible-inventory`로 대상 host/group를 선택하고, `community.sops` 환경에서 `ansible-inventory --host` 결과에 `ENC[...]` 가 남을 수 있기 때문에 평문이 필요한 `ssh_host / ssh_port / ssh_host_fingerprint`만 임시 `ansible-playbook`으로 materialize 한다
- `_ansible-exec.yml` 은 inventory resolver 성공을 전제로 하며, prod caller 쪽 legacy SSH fallback은 두지 않는다

## Ansible 구조

### 디렉토리 레이아웃

```text
infra/ansible/
├── ansible.cfg                          # SOPS 플러그인, SSH 파이프라이닝
├── collections/requirements.yml         # community.docker, community.sops
├── inventories/
│   ├── dev/
│   │   ├── hosts.yml                    # 호스트 그룹 + ansible_port / ansible_user (ansible_host는 SOPS)
│   │   └── group_vars/all/
│   │       ├── main.yml                 # 평문 변수 (배포 설정, 컨테이너 구성)
│   │       └── secrets.sops.yml         # SOPS 암호화 (ansible_host, ssh_host_fingerprint, DB, 도메인 등)
│   └── prod/
│       ├── hosts.yml                    # 호스트 그룹 + ansible_port / ansible_user (ansible_host는 SOPS)
│       └── group_vars/all/
│           ├── main.yml                 # 평문 변수
│           └── secrets.sops.yml         # SOPS 암호화 (ansible_host, ssh_host_fingerprint, DB, 도메인 등)
├── playbooks/
│   ├── foundation.yml                   # 인프라 기반 스택
│   ├── deploy.yml                       # 앱 배포
│   ├── rollback.yml                     # 롤백
│   └── secret.yml                       # 시크릿만 동기화
├── roles/
│   ├── foundation_stack/                # docker-compose 기반 기초 서비스
│   │   └── templates/foundation.compose.yml.j2
│   ├── nginx_base_config/               # nginx 설정 렌더링 + 프로모션
│   │   └── templates/default.conf.j2
│   ├── nginx_config_helper/             # update-nginx-config.py 배포 helper
│   ├── app_secret/                      # SOPS → properties 파일
│   ├── app_scripts/                     # 배포 디렉토리 + nginx helper 준비
│   ├── app_release/                     # 릴리즈 메타데이터
│   ├── app_bluegreen/                   # blue-green 핵심 로직
│   ├── app_stopstart/                   # stop-start 배포
│   ├── app_healthcheck/                 # 헬스체크
│   ├── app_cleanup/                     # 메타데이터 승격 + 이미지 정리
│   └── app_rollback/                    # 롤백 로직
```

설명:
- 공용 nginx helper `update-nginx-config.py`는 더 이상 `infra/ansible/files/`에 두지 않고 `roles/nginx_config_helper/files/`로 이동했다.
- foundation / nginx 템플릿도 전역 `templates/`가 아니라 각 role의 `templates/` 안에서 관리한다.
- `deploy.yml`은 blue-green 모듈에서 `app_bluegreen`을 직접 import하고, `app_rollback`은 상대 `import_tasks` 대신 `import_role` + 명시적 vars 전달 구조를 사용한다.

### Foundation marker contract

- `foundation.yml`은 `foundation_stack`과 선택적 `nginx_base_config`가 모두 성공한 뒤 `{{ deployment_dir }}/.foundation-applied` marker를 생성한다.
- marker는 foundation의 마지막 `post_tasks`에서 쓰이므로, foundation 도중 실패하면 새 marker가 생성되지 않는다. inventory나 foundation 변수 변경 후에는 `foundation.yml`을 다시 실행해 marker를 갱신한다.
- marker 내용은 YAML 형식이며 `applied_at`, `commit_sha`, `deploy_environment`, `foundation_mysql_enabled`, `foundation_redis_enabled`, `foundation_manage_nginx`를 기록한다.
- `deploy.yml`과 `rollback.yml`은 앱 role을 실행하기 전에 같은 marker를 `stat`/`slurp`로 확인한다. marker가 없으면 foundation이 적용되지 않은 host로 보고 즉시 실패하고, marker가 있으면 내용을 diagnostic log로 출력한다.
- `deploy-dev.yml`과 `deploy-prod.yml`은 deploy job 전에 `foundation` job을 실행한다. reusable workflow `_ansible-exec.yml`에 `module: foundation`을 전달하되, SSH metadata resolver는 현재 단일 host inventory contract를 재사용하기 위해 `connection_module`을 조회한다. 기본값은 `apis`이며, inventory 대표 호스트가 바뀌면 GitHub environment/repository variable `DEV_FOUNDATION_CONNECTION_MODULE` / `PROD_FOUNDATION_CONNECTION_MODULE`로 `${connection_module}_servers` 조회 대상을 바꾼다.
- foundation job과 deploy/rollback job은 각각 기존 `deploy-dev-runtime-${{ github.ref }}` / `prod-runtime` concurrency group을 공유하므로 foundation, deploy, rollback이 같은 런타임에서 겹치지 않는다.

### Seed placeholder upstreams

`nginx_base_config`는 첫 foundation 실행 시 backend/admin/actuator upstream fragment가 없으면
`nginx_seed_placeholder_host:nginx_seed_placeholder_port`(기본 `127.0.0.1:65535`)를 가리키는
placeholder upstream을 생성한다. 이는 다음 두 가지를 보장한다.

1. **`nginx -t` 통과**: nginx가 시작하려면 모든 referenced upstream이 정의되어 있어야 한다.
   placeholder가 없으면 첫 deploy 전 nginx 부팅/검증이 실패한다.
2. **Blackhole 동작**: `127.0.0.1:65535`는 의도적으로 도달 불가능한 endpoint다.
   placeholder가 실수로 트래픽을 받더라도 실제 애플리케이션으로 라우팅되지 않는다.

placeholder는 `app_bluegreen`(apis)이나 `app_stopstart/admin_nginx_route`(admin)이 첫 배포될 때
`upsert-upstream`으로 실제 컨테이너 이름과 포트로 덮어쓴다. inventory에서 host/port를 변경할 수는
있지만, nginx pre-deploy validation + blackhole 계약을 깨지 않는 값으로만 유지해야 한다.

### Nginx fragment mapping contract

`nginx_fragments` inventory 값은 upstream 이름과 generated fragment 파일명을 묶는 canonical mapping이다.
`nginx_base_config`, `app_bluegreen`, `app_stopstart/admin_nginx_route`는 `backend.conf` 같은 파일명을
직접 하드코딩하지 않고 이 mapping의 `fragment_file`을 참조한다. `foundation.yml`, `deploy.yml`,
`rollback.yml`은 preflight에서 mapping 누락/빈 값/중복 파일명을 즉시 실패시킨다.

운영 중인 host에서 `fragment_file`을 바꾸면 기존 `nginx/generated` source/target fragment와 `.bak`
복구 경계가 서로 다른 경로를 보게 될 수 있다. 따라서 `nginx_fragments`는 일반 운영 변수라기보다
read-only contract로 취급하고, 변경이 필요하면 기존 fragment 이동/정리 계획과 함께 별도 마이그레이션으로
진행한다.

### Playbook 흐름

```mermaid
flowchart TD
    subgraph Foundation["foundation.yml"]
        FS["foundation_stack<br/>nginx + mysql + redis"]
        NBC["nginx_base_config<br/>설정 렌더링 + seed"]
        MARK[".foundation-applied<br/>post_tasks marker"]
        FS --> NBC --> MARK
    end

    subgraph Deploy["deploy.yml"]
        VAL["pre_tasks: validate"]
        FM["pre_tasks: foundation marker check"]
        SEC["app_secret<br/>SOPS 복호화"]
        SCR["app_scripts<br/>유틸리티 배포"]
        REL["app_release<br/>메타데이터 기록"]
        BG["app_bluegreen<br/>(apis)"]
        SS["app_stopstart<br/>(admin, batch)"]
        HC["app_healthcheck"]
        NR["admin_nginx_route<br/>(admin만)"]
        CL["app_cleanup<br/>메타데이터 승격"]

        VAL --> FM --> SEC --> SCR --> REL
        REL -->|"blue_green"| BG
        REL -->|"stop_start"| SS
        BG --> HC
        SS --> HC
        HC --> NR --> CL
    end

    subgraph Rollback["rollback.yml"]
        RFM["pre_tasks: foundation marker check"]
        RB["app_rollback<br/>이전 릴리스로 런타임 복원"]
        RHC["app_healthcheck<br/>복원된 런타임 검증"]
        RNR["admin_nginx_route<br/>(admin만)"]
        RR["previous.json → current.json<br/>메타데이터 정합성 복원"]
    end

    RFM --> RB --> RHC --> RNR --> RR
```

### 모듈별 배포 전략

| 모듈 | 배포 모드 | 포트 | 다운타임 | Nginx 라우팅 |
|------|----------|------|---------|-------------|
| **apis** | blue_green | 4001 | Zero-downtime | `/ → backend` upstream 전환 |
| **admin** | stop_start | 4000 | 있음 | `/admin/ → admin_backend` |
| **batch** | stop_start | 4002 | 있음 | 없음 (내부 스케줄러) |

### Blue-Green 배포 상세 (apis)

```mermaid
sequenceDiagram
    participant A as Ansible
    participant N as Nginx
    participant Blue as apis-blue
    participant Green as apis-green

    Note over A: active slot = blue
    A->>Green: 새 이미지로 컨테이너 시작
    loop Health Check (max 60s)
        A->>Green: GET /actuator/health
    end
    Green-->>A: 200 OK

    A->>A: upsert upstream → Green
    A->>N: nginx -t && nginx -s reload
    Note over N: 트래픽 전환: Blue → Green

    loop Smoke Test (max 30s)
        A->>N: GET /api/main
    end
    N-->>A: 200 OK

    A->>Blue: 컨테이너 제거
    A->>A: current-slot ← "green"

    Note over A: 실패 시 자동 롤백
    rect rgb(255, 230, 230)
        A->>A: nginx config 복원 (.bak)
        A->>A: upstream fragment 복원
        A->>N: nginx -t && reload
        A->>Green: 실패한 컨테이너 제거
    end
```

## SOPS 시크릿 관리

### 암호화 체인

```mermaid
flowchart LR
    subgraph Encryption["암호화 (개발자 로컬)"]
        Plain["평문 시크릿"] -->|"sops -e"| Encrypted["secrets.sops.yml<br/>(age 암호화)"]
    end

    subgraph CICD["CI/CD (GitHub Actions)"]
        GHSecret["AGE_SECRET_KEY<br/>(Environment Secret: dev/prod)"]
        GHSecret -->|"SOPS_AGE_KEY"| Decrypt["환경별 자동 복호화<br/>(community.sops 플러그인)"]
        Decrypt --> Props["application-{profile}-secret.properties"]
    end

    subgraph Server["EC2 서버"]
        Props -->|"/opt/beat/secret/"| Container["Spring Boot 컨테이너<br/>volume mount (:ro)"]
    end

    Encrypted -->|"git push"| CICD
```

### age 키 관리

```mermaid
flowchart TD
    subgraph Keys["age 키 쌍"]
        PK1["Public Key (본인)<br/>age1xxxxx..."]
        SK1["Private Key (본인)<br/>AGE-SECRET-KEY-1..."]
        PK2["Public Key (팀원)<br/>age1xxxxx..."]
        SK2["Private Key (팀원)<br/>AGE-SECRET-KEY-1..."]
    end

    subgraph SopsYaml[".sops.yaml"]
        Recipients["recipients:<br/>PK1, PK2, ..."]
    end

    subgraph Decrypt["복호화"]
        D1["본인: SK1으로 복호화 ✅"]
        D2["팀원: SK2로 복호화 ✅"]
        D3["CI: SK1으로 복호화 ✅"]
    end

    PK1 & PK2 --> Recipients
    Recipients -->|"sops -e"| ENC["암호화된 파일<br/>(모든 recipient용 사본 포함)"]
    ENC --> D1 & D2 & D3
```

**키 1개로 복호화 가능**: SOPS는 각 recipient의 public key로 별도 암호화 사본을 만든다. 복호화 시 자신의 private key 하나만 있으면 된다.

### secrets.sops.yml에 저장되는 항목

| 카테고리 | 변수 | 설명 |
|---------|------|------|
| **인프라** | `ansible_host` | 서버 IP (평문 노출 방지) |
| **인프라** | `nginx_server_name` | 도메인 |
| **인프라** | `letsencrypt_cert_name` | SSL 인증서 도메인 |
| **인프라** | `actuator_allow_cidrs` | actuator 접근 허용 CIDR 목록 |
| **인프라** | `actuator_port` | actuator 포트 (시크릿 경로 일부) |
| **인프라** | `actuator_path` | actuator 경로 (시크릿 경로 일부) |
| **DB** | `mysql_root_password`, `mysql_database`, `mysql_user`, `mysql_password` | MySQL 접속 정보 |
| **앱** | `app_secret_content` | Spring Boot 시크릿 properties 전체 |

`main.yml`에는 배포 모드, 컨테이너 이름, 포트, 경로 등 **노출되어도 무방한 설정값**만 남긴다.

### 팀원 추가 절차

```bash
# 1. 팀원이 age 키 생성
age-keygen -o ~/Library/Application\ Support/sops/age/keys.txt
# 출력: public key: age1xxxxxxxxx...

# 2. .sops.yaml에 팀원 public key 추가 (쉼표 구분)
# creation_rules:
#   - path_regex: ...
#     age: >-
#       age1본인publickey...,
#       age1새팀원publickey...

# 3. 기존 시크릿 파일 재암호화 (기존 키 소유자가 실행)
sops updatekeys infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml
sops updatekeys infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml

# 4. 커밋 & 푸시
```

### 시크릿 편집

```bash
# 복호화 후 편집기 열기 (저장 시 자동 재암호화)
sops infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml

# 특정 값만 추출
sops -d --extract '["actuator_port"]' infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml
```

## 서버 구성

### Docker 컨테이너 구조

```mermaid
flowchart TB
    subgraph Network["Docker Network: beat-network"]
        Nginx["nginx:alpine<br/>:80 / :443"]
        ApisBlue["apis-blue<br/>:4001"]
        ApisGreen["apis-green<br/>:4001<br/>(배포 시만)"]
        Admin["admin<br/>:4000"]
        Batch["batch<br/>:4002"]
        MySQL["mysql:8.4.5<br/>:3306<br/>(dev only)"]
        Redis["redis:alpine"]
    end

    Client["Client"] -->|"HTTPS"| Nginx
    Nginx -->|"/ → backend"| ApisBlue
    Nginx -->|"/admin/ → admin_backend"| Admin
    Nginx -->|"/actuator-path/"| ApisBlue
    ApisBlue --> MySQL
    ApisBlue --> Redis
    Admin --> MySQL
    Admin --> Redis
    Batch --> MySQL
    Batch --> Redis

    style ApisGreen stroke-dasharray: 5 5
```

### Nginx Generated Fragment 시스템

```mermaid
flowchart LR
    subgraph Template["default.conf.j2 (정적)"]
        UI["include .../upstreams/*.conf"]
        RI["include .../routes/*.conf"]
        Act["location /actuator-path/ { allow/deny }"]
        Root["location / { proxy_pass backend }"]
    end

    subgraph Upstreams["generated/upstreams/*.conf"]
        B["backend.conf -> upstream backend { apis-blue:4001 }"]
        AB["admin_backend.conf -> upstream admin_backend { admin:4000 }"]
        AC["actuator.conf -> upstream actuator { apis-blue:port }"]
    end

    subgraph Routes["10-managed.conf"]
        AR["location /admin/ { → admin_backend }"]
    end

    UI -.->|"include"| Upstreams
    RI -.->|"include"| Routes

    subgraph Tool["update-nginx-config.py"]
        BS["bootstrap-includes<br/>include 마커 삽입"]
        UU["upsert-upstream<br/>upstream 블록 갱신"]
        ER["ensure-route<br/>location 블록 갱신"]
    end

    Tool -->|"관리"| Upstreams & Routes
```

### 서버 파일시스템 레이아웃

```text
/home/ubuntu/deployment/                    # Ansible 작업 디렉토리
├── docker-compose.yml                      # foundation 렌더링
├── update-nginx-config.py                  # nginx fragment 관리
└── nginx/
    ├── default.conf                        # 후보 설정 (source, 다음 promotion 입력)
    ├── generated-source/                   # 후보 fragment (source, 다음 promotion 입력)
    │   ├── upstreams/backend.conf
    │   ├── upstreams/admin_backend.conf
    │   ├── upstreams/actuator.conf
    │   └── routes/10-managed.conf
    ├── conf.d/default.conf                 # bind mount target → /etc/nginx/conf.d/default.conf
    └── generated/                          # bind mount target → /etc/nginx/generated
        ├── upstreams/backend.conf
        ├── upstreams/admin_backend.conf
        ├── upstreams/actuator.conf
        └── routes/10-managed.conf

/opt/beat/
├── secret/
│   └── application-{profile}-secret.properties
└── releases/{module}/
    ├── current-slot                        # blue/green (apis만)
    ├── current.json                        # 현재 배포 메타데이터
    └── previous.json                       # 이전 배포 (롤백용)
```

### Release metadata schema

`app_release`는 배포 시작 시 `pending.json`을 쓰고, 배포/검증이 끝난 뒤 `app_cleanup`이 이를 `current.json`으로 승격한다. 기존 `current.json`은 `previous.json`으로 보존되어 rollback 입력이 된다. `app_rollback`은 rollback 전에 기존 `current.json`을 `reverted-<UTC>.json`으로 archive한다.

| 필드 | 의미 | 출처 / provenance |
|------|------|-------------------|
| `module` | 배포 대상 모듈 (`apis`, `admin`, `batch`) | GitHub workflow matrix/input이 Ansible extra var로 전달 |
| `image` | 실제 실행할 Docker image 전체 이름 | deploy/rollback workflow에서 전달한 image extra var |
| `image_tag` | image tag 또는 release tag | dev는 `dev-{GITHUB_SHA}`, prod는 release tag 기준 |
| `commit_sha` | 배포 기준 Git commit SHA | GitHub run에서 해석한 commit SHA (`github.sha` 또는 release ref resolve 결과), 없으면 `unknown` |
| `deploy_actor` | 배포를 트리거한 GitHub actor | GitHub workflow의 actor 값, 없으면 `unknown` |
| `deploy_environment` | Ansible inventory 환경 (`dev`/`prod`) | inventory `deploy_environment` |
| `created_at` | metadata 생성 시각 | Ansible controller(GitHub runner)에서 계산한 UTC (`now(utc=true, ...)`) |

`created_at`은 원격 EC2의 시스템 시간이 아니라 controller UTC이다. 서버 로그와 비교할 때는 GitHub runner에서 metadata가 생성된 시각으로 해석한다.

## GitHub Secrets

### 필수 (Environment: dev)

| Secret | 용도 |
|--------|------|
| `AGE_SECRET_KEY` | dev 환경 SOPS 복호화용 age private key |
| `DEV_DOCKER_LOGIN_USERNAME` | Docker Hub 사용자명 |
| `DEV_DOCKER_LOGIN_ACCESSTOKEN` | Docker Hub 액세스 토큰 |
| `DEV_SSH_HOST` | dev 서버 IP |
| `DEV_SSH_PORT` | dev 서버 SSH 포트 |
| `DEV_SSH_PRIVATE_KEY` | dev 서버 SSH 비밀키 |
| `DEV_SSH_HOST_FINGERPRINT` | dev 서버 SSH 호스트 지문 (`SHA256:...`) |

### 필수 (Environment: prod)

| Secret | 용도 |
|--------|------|
| `AGE_SECRET_KEY` | prod 환경 SOPS 복호화용 age private key |
| `PROD_DOCKER_LOGIN_USERNAME` | Docker Hub 사용자명 |
| `PROD_DOCKER_LOGIN_ACCESSTOKEN` | Docker Hub 액세스 토큰 |
| `PROD_SSH_HOST` | prod 서버 IP |
| `PROD_SSH_PORT` | prod 서버 SSH 포트 |
| `PROD_SSH_PRIVATE_KEY` | prod 서버 SSH 비밀키 |
| `PROD_SSH_HOST_FINGERPRINT` | prod 서버 SSH 호스트 지문 (`SHA256:...`) |

### 선택 (Repository-level 또는 Environment-level)

| Secret | 용도 |
|--------|------|
| `SLACK_WEBHOOK_URL` | 배포 성공/실패 Slack 알림 (없으면 skip) |
| `ACTION_TOKEN` | Release Drafter용 GitHub 토큰 |

### SSH Host Fingerprint 확인 방법

```bash
# 로컬 터미널에서 실행 (서버 접속 불필요, 공개키 조회)
ssh-keyscan -p 22 <서버IP> 2>/dev/null | ssh-keygen -lf - -E sha256
# 출력에서 ED25519의 SHA256:... 값을 사용
```

> **참고**: `DEV_SSH_HOST`와 `ansible_host`(secrets.sops.yml)는 동일한 IP이다.
> 전자는 GHA runner의 SSH known_hosts 설정에, 후자는 Ansible inventory 접속에 사용된다.

### SSH pipelining + sudo `requiretty` caveat

`infra/ansible/ansible.cfg`는 SSH pipelining을 켠다. Ubuntu 22.04 계열 기본 EC2 AMI에서는 `become: true`와 함께 정상 동작하지만, 일부 커스텀 AMI나 레거시 sudoers 정책에서 `Defaults requiretty`가 켜져 있으면 Ansible pipelining + sudo 조합이 실패할 수 있다.

- Ubuntu 기본 AMI처럼 `requiretty`가 없는 환경: 현재 설정 유지 (`pipelining = True`)
- 커스텀 AMI에서 sudo가 TTY를 요구하는 환경: sudoers에서 `requiretty`를 끄거나, 해당 inventory/ansible.cfg에서 pipelining을 비활성화한 뒤 재검증
- 증상: SSH 접속은 성공하지만 `become` 태스크가 sudo/TTY 관련 오류로 실패

운영 AMI를 교체할 때는 foundation/deploy/rollback syntax check만으로 충분하지 않다. 실제 `become` 태스크가 포함된 dry-run 또는 제한된 smoke deploy로 pipelining 호환성을 확인한다.

## 로컬 개발

### 사전 준비

1. **SOPS + age 설치**
   ```bash
   # macOS
   brew install sops age
   ```

2. **age 키 생성** (최초 1회)
   ```bash
   age-keygen -o ~/Library/Application\ Support/sops/age/keys.txt
   # 출력된 public key를 기존 키 소유자에게 전달
   # → .sops.yaml에 추가 + sops updatekeys 실행 필요
   ```

3. **시크릿 파일 생성**
   ```bash
   ./scripts/generate-local-dev-secret.sh
   # → secret/application-dev-secret.properties 생성
   ```
   이 스크립트는 SOPS로 `secrets.sops.yml`을 복호화하여 로컬용 properties 파일을 만든다.
   dev 서버 배포용 시크릿은 Docker network 기준 값을 사용하므로, 로컬 실행용 파일을 만들 때
   `DEV_DB_URL`과 `DEV_REDIS_HOST`는 자동으로 로컬 실행 기준 값으로 오버라이드된다.
   `DEV_DB_URL`은 암호화된 원본 값에서 host만 `localhost`로 치환하므로 별도 입력 없이 바로 실행할 수 있다.

### 로컬 실행

```bash
# 로컬 MySQL, Redis가 필요
# MySQL: secret/application-dev-secret.properties의 DEV_DB_URL
# Redis: localhost:6379

# 모듈별 실행
./gradlew :apis:bootRun
./gradlew :admin:bootRun
./gradlew :batch:bootRun
```

### 로컬 검증

```bash
# 전체 테스트
./gradlew test

# 배포 계약 테스트
./gradlew :test --tests com.beat.RootRetirementContractTest

# Ansible syntax check
cd infra/ansible
ansible-playbook playbooks/foundation.yml -i inventories/dev/hosts.yml --syntax-check
ansible-playbook playbooks/deploy.yml -i inventories/dev/hosts.yml --syntax-check \
  -e module=apis -e image=test -e image_tag=test
ansible-playbook playbooks/deploy.yml -i inventories/prod/hosts.yml --syntax-check \
  -e module=admin -e image=test -e image_tag=test
ansible-playbook playbooks/rollback.yml -i inventories/dev/hosts.yml --syntax-check -e module=batch
ansible-playbook playbooks/rollback.yml -i inventories/prod/hosts.yml --syntax-check -e module=apis
```

## 환경별 차이

| 항목 | Dev | Prod |
|------|-----|------|
| 배포 트리거 | develop push / workflow_dispatch | release.published (자동, shared tag) |
| 이미지 태그 | `dev-{SHA}` + `dev-latest` | `{release_tag}` (예: `v1.2.3`) + `prod-latest` |
| MySQL | Docker 컨테이너 (foundation) | 비활성 (`foundation_mysql_enabled: false`, 외부 RDS) |
| Redis 컨테이너명 | `redis` | `beat-prod-redis` |
| 도메인 | `secrets.sops.yml`의 `nginx_server_name` 참조 | 동일 |
| 롤백 | 재배포로 대체 | rollback-prod.yml (수동) |
| concurrency | `deploy-dev-runtime-{ref}` (브랜치별) | `prod-runtime` (전역 락) |

## Rollback

prod 전용 `rollback-prod.yml` 워크플로우가 제공된다.

```mermaid
flowchart LR
    A["workflow_dispatch<br/>(module + release_ref 선택)"] --> B["release_ref checkout<br/>commit SHA 고정"]
    B --> C["_ansible-exec.yml<br/>checkout_ref = commit SHA<br/>playbook: rollback.yml"]
    C --> D["previous.json 읽기"]
    D --> E{"deploy_mode?"}
    E -->|"blue_green"| F["반대 슬롯으로<br/>BG 재배포"]
    E -->|"stop_start"| G["이전 이미지로<br/>컨테이너 재생성"]
    F & G --> H["app_healthcheck"]
    H --> I["admin nginx route 갱신<br/>(admin만)"]
    I --> J["previous.json → current.json"]
```

- `previous.json`이 없으면 롤백 불가 (assert 실패)
- 롤백은 `release_ref` 입력(릴리스 태그 또는 커밋 SHA)을 먼저 immutable commit SHA로 해석하고, `_ansible-exec.yml`의 `checkout_ref`로 전달해 prod 배포 때와 같은 코드 기준에서 실행한다.
- 롤백 후 `app_healthcheck`와 nginx route 갱신이 모두 성공해야 `current.json`을 다시 live 상태가 된 이전 배포의 메타데이터로 복원한다.
- 이 승격은 선택사항이 아니다. 다음 deploy의 `app_cleanup`이 항상 `current.json -> previous.json`을 수행하므로, rollback 직후 `current.json`이 실제 live 릴리스를 가리키지 않으면 다음 rollback 대상이 틀어진다.
- 반대로 post-rollback healthcheck 또는 route 갱신이 실패하면 metadata promotion을 차단하여 검증되지 않은 runtime 상태를 `current.json`에 기록하지 않는다.
- blue-green 모듈은 `run_switch.yml`을 재활용하여 역방향 전환을 수행한다

### Nginx source/target contract

- `deployment/nginx/default.conf` 와 `deployment/nginx/generated-source/**` 는 후보(source) 설정이다. helper와 seed 로직이 이 경로를 갱신하고, 다음 promotion의 입력으로 재사용된다.
- `deployment/nginx/conf.d/**` 와 `deployment/nginx/generated/**` 는 nginx 컨테이너에 bind mount되는 실제 적용(target) 설정이다. 컨테이너 안에서는 각각 `/etc/nginx/conf.d` 와 `/etc/nginx/generated` 로 보인다.
- 예전 named volume(`nginx_legacy_config_volume_name`, 기본 `nginx-config-volume`)은 신규 target contract에서 제외했다. foundation은 첫 실행 시 Ansible `community.docker.docker_volume_info`로 legacy mountpoint를 해석하고, 그 안의 `conf.d`/`generated` 내용을 deployment-owned bind mount 경로로 한 번 복사한다. 이후 `.bind-mount-migrated-from-<legacy-volume-name>` marker로 재실행 시 overwrite를 막는다.
- Nginx 관련 role은 실패 시 target만이 아니라 source도 함께 복원해야 한다. 그렇지 않으면 다음 실행에서 오염된 source가 다시 target으로 승격될 수 있다.
