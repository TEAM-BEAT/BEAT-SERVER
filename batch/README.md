# batch module

> 이 문서는 `batch` 모듈의 최종 To-Be 계약을 정의한다. 현재 배치 lane은 전환기지만, 최종적으로 스케줄/배치 책임은 `batch`에만 남아야 한다.

## 역할

- 스케줄러, 배치 잡, 유지보수 작업의 유일한 실행 진입점이다.
- 대량 처리, 정기 작업, 비동기 유지보수 흐름을 소유한다.
- 팀 컨벤션상 `Job/Runner -> Facade -> Application Service -> Domain` 흐름을 따른다.
- 외부 발송/저장 구현은 `infra`를 통해 사용한다.

## 허용 의존성

- `domain`
- `infra`
- `global-utils`
- `observability`

## 금지 규칙

- `project(":")` 직접 의존 금지
- `apis`, `admin` 직접 의존 금지
- `gateway` 직접 의존 금지
- 사용자용 Controller/API DTO 보유 금지
- 전역 스캔에 기대는 구조 금지
- 스케줄 소유권을 다른 실행 모듈과 중복 활성화 금지

## As-Is 패키지 구조

```text
batch/
  src/main/kotlin/com/beat/batch/
    BatchApplication.kt
    config/
      InfraConfig.kt           # @EnableInfraBaseConfig(JPA, QUERY_DSL, ASYNC)

legacy root:
  src/main/java/com/beat/global/common/scheduler/application/
  src/main/java/com/beat/domain/**/application/   # 일부 scheduled service 포함
```

설명:
- 현재 `batch` 모듈은 앱 진입점과 최소 설정만 가진 얇은 lane이다.
- `InfraConfig.kt`가 `@EnableInfraBaseConfig`로 필요한 infra 설정 그룹을 선택적으로 import한다.
- 전환기 동안 `implementation(project(":"))` root 의존이 남아 있다. 최종적으로 제거 대상이다.
- 실제 scheduler/service 코드의 상당 부분은 아직 root legacy 패키지에 남아 있다.
- 공통 scheduler application 패키지뿐 아니라 일부 domain application service에도 scheduling 책임이 남아 있다.

## To-Be 패키지 구조

```text
com.beat.batch.<context>/
  job/
  facade/
  application/
    service/
      command/
      query/   # 필요할 때만
    dto/
    exception/
  config/
```

## 서비스 / CQRS 규칙

- `Facade`는 하나로 유지하고 배치 실행 시나리오 조합을 맡는다.
- 실제 잡 실행은 대부분 command 성격이므로 `application/service/command`를 기본으로 둔다.
- 배치 조회/리포트/통계가 필요할 때만 `application/service/query`를 추가한다.
- DTO는 command/query로 나누지 않고 `application/dto` 아래에서 관리한다.
- 배치 애플리케이션 문맥의 에러 코드와 예외는 `application/exception`에 둔다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- Repository는 지금 즉시 분리하지 않고, 복잡한 조회 전용 구현이 필요할 때만 infra query 레이어를 추가한다.

## 최종 목표

- 스케줄 소유권이 `batch` 하나로 고정된다.
- 운영 작업은 HTTP lane 없이 독립 실행 가능하다.
- 배치 로직이 사용자 API 코드에 기대지 않는다.
