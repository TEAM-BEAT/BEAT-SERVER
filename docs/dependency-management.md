# 의존성 관리 런북

의존성 업데이트를 자동화하되, PR 생성·취약점 검증·resolved graph 갱신의 책임을 분리합니다. 이번 자동화 인프라 정비 자체는 애플리케이션 공개 API 및 런타임 동작을 변경하지 않습니다. 이후 Renovate dependency update PR의 런타임 영향은 각 PR의 CI와 사람 리뷰에서 별도로 검증합니다.

## 책임 경계

| 도구 | 책임 | 병합 판단 |
|---|---|---|
| Dependabot Alerts | 취약점 탐지와 alert 제공 | 직접 PR을 만들지 않음 |
| Renovate | 일반·보안 dependency PR 생성의 유일한 bot | PR별 CI와 사람 승인 필요 |
| Dependency Review | PR에서 새로 유입되는 runtime dependency의 HIGH 이상 차단 | `HIGH`와 `CRITICAL` 실패 |
| Trivy | JVM image와 Lambda production dependency의 fix 가능한 취약점 검증 | `HIGH`/`CRITICAL` 실패 |
| Dependency Submission | PR resolved snapshot 제공 + 병합 후 default-branch dependency graph 갱신 | 직접 PR을 만들거나 dependency를 수정하지 않음 |

## 업데이트 정책

일반 업데이트는 `Asia/Seoul` 기준 월요일에만 새 PR을 만듭니다. 일반 PR은 최대 5개, 시간당 최대 2개이며 `dependencies` 라벨을 붙입니다. major 업데이트는 다른 업데이트와 묶지 않습니다. 자동 병합은 사용하지 않고 승인 1명과 필수 CI를 통과시킨 뒤 병합합니다.

Spring Boot와 Spring Cloud는 일반 라이브러리 그룹이 아니라 managed platform baseline입니다. Renovate는 검증된 Boot `4.0.x` release line과 Cloud `2025.1.x` release train 안에서만 stable patch PR을 만듭니다. Boot `4.1.x` 또는 다른 Cloud release train으로의 변경은 자동 PR 대상이 아니며, 변경 시점의 공식 Spring compatibility 정보를 확인하고 별도 migration PR과 전체 CI를 거쳐 사람이 baseline 제한을 변경합니다. platform baseline PR에는 `platform-baseline` 라벨을 추가합니다.

보안 업데이트는 취약점 alert를 기준으로 즉시 처리합니다. 일반 업데이트의 schedule과 hourly limit을 적용하지 않으며, 보안 PR에도 `dependencies`와 `security` 라벨을 붙입니다. 보안 PR에는 별도 동시 PR 예산(최대 5개)을 사용합니다. 보안 업데이트도 자동 병합하지 않고 승인 1명과 필수 CI를 요구합니다.

Gradle build tooling 중 코드에서 동적으로 marker/version을 조립하던 항목은 Renovate의 정적 분석이 package 좌표와 version을 연결할 수 있도록 version catalog에 명시적인 library 좌표를 둡니다. build logic은 같은 catalog entry를 실제로 사용하므로 중복 버전 소스나 unused alias를 만들지 않습니다.

## PR 검증

default branch인 `develop`을 대상으로 같은 저장소에서 생성된 PR은 Gradle `runtimeClasspath` resolved graph를 Dependency Submission으로 먼저 제출한 뒤 Dependency Review를 실행합니다. 이 PR snapshot은 default branch graph를 대체하지 않고, Dependency Review가 base와 PR의 resolved dependency 차이를 비교하는 입력으로 사용합니다. Dependency Review는 runtime scope의 신규 dependency 변경을 검사하고 `HIGH` 이상을 차단합니다. snapshot 경고는 같은 저장소 PR에서만 최대 300초 재시도합니다.

`develop`에서 `main`으로 승격하는 release PR은 Dependency Snapshot과 Dependency Review를 의도적으로 skip합니다. GitHub Dependency Graph는 default branch snapshot만 repository dependency 결과의 기준으로 사용하므로 non-default `main` snapshot은 release PR의 비교 기준을 만들지 못합니다. release 대상 커밋은 `develop` 유입 시 Dependency Review를 이미 통과했으며, release PR에서는 Gradle check, Sonar, JVM image Trivy, Lambda security 등 산출물 검증을 다시 수행합니다.

긴급 수정이 `main`에 직접 병합되어 두 branch의 이력이 갈라졌다면 다음 release 전에 `main`을 `develop`로 merge commit 방식으로 동기화합니다. 충돌은 최신 검증 정책을 가진 `develop` 내용을 기준으로 해결하며, squash하면 `main` ancestry가 기록되지 않아 같은 충돌이 반복되므로 이 동기화에만 squash를 사용하지 않습니다.

병합 후에는 default branch인 `develop`의 push 기반 Dependency Submission이 최종 resolved graph를 GitHub Dependency Graph에 제출하고 Dependabot Alerts의 inventory를 갱신합니다. `main`은 non-default branch이므로 push snapshot을 별도로 제출하지 않습니다.

JVM 애플리케이션은 실제 Docker image를 Trivy image scan으로 검사합니다. Lambda는 `ubuntu-24.04-arm` ARM64 runner에서 Node.js 22로 production/optional dependency를 `--ignore-scripts`와 함께 설치하고, `@img/sharp-linux-arm64`, `@img/sharp-libvips-linux-arm64` 존재 여부와 실제 `sharp` native module import를 확인합니다. 이후 Lambda directory의 npm dependency graph를 Trivy filesystem scan으로 검사합니다. 이 검사는 ARM64 실행 성능 benchmark가 아니라, 배포 대상 아키텍처에서 native dependency가 load 가능한지와 production dependency에 fix 가능한 HIGH/CRITICAL 취약점이 없는지를 검증합니다.

실제 Ansible Lambda 패키징도 `npm ci --ignore-scripts`를 사용하고 ARM64 Sharp optional package의 존재를 확인합니다. Sharp `0.35+`는 install lifecycle script 없이 prebuilt optional package를 사용하므로, 패키징 중 third-party lifecycle script 실행을 허용할 필요가 없습니다.

`develop` 대상 외부 fork PR에서는 read-only token으로 Gradle snapshot을 제출하지 않고 snapshot job을 정상적으로 skip합니다. Dependency Review의 manifest/lock 기반 정적 검사와 read-only CI는 계속 실행합니다.

## 외부 fork dependency PR

`fork-dependency-policy`는 외부 fork PR의 changed-files 목록을 GitHub REST API로 read-only 조회합니다. 다음 dependency-sensitive 경로를 변경한 외부 fork PR은 `verify`에서 실패시켜 maintainer-owned branch 전환을 강제합니다.

- Renovate/Dependabot 설정
- Gradle wrapper, version catalog, build scripts와 `build-logic`
- npm manifest/lockfile
- Dockerfile
- GitHub Actions workflow/composite action
- Python/Ansible requirements

같은 저장소 PR과 dependency-sensitive 파일을 바꾸지 않은 일반 fork 코드 PR은 이 정책 job을 통과합니다. dependency-sensitive fork PR은 maintainer-owned branch로 커밋을 옮긴 뒤 resolved dependency snapshot과 전체 `verify`를 다시 실행하고, 필수 CI와 사람 승인을 확인한 뒤 병합합니다.

## Transitive CVE 대응

직접 선언하지 않은 dependency의 CVE는 다음 순서로 처리합니다.

1. 부모 BOM 또는 platform을 안전한 버전으로 올립니다.
2. 해결되지 않으면 직접 dependency를 업데이트합니다.
3. Spring Boot 또는 Spring Cloud BOM이 아직 수정 버전을 제공하지 않으면 최소 범위의 managed-version property override 또는 임시 Gradle constraint를 추가합니다. constraint 주석과 PR 설명에 CVE 번호, 적용 근거, 제거 조건(안전 버전 또는 BOM 반영)을 남깁니다.
4. 안정적인 수정 버전이 아직 없으면 constraint를 유지하고 issue와 SLA로 추적합니다. alert를 임의로 무시하지 않습니다.

안전한 BOM/platform 버전이 제공되면 임시 constraint를 제거하는 후속 PR을 만듭니다.

release-line 제한 때문에 수정 버전이 다음 Boot line 또는 다른 Cloud train에만 있는 경우 Renovate PR은 생성하지 않을 수 있습니다. 이때 alert는 유지하고, 현재 line의 안전한 BOM patch를 기다리거나 최소 범위 override를 별도 보안 PR로 판단합니다. release-line 자체를 올리는 경우에는 compatibility 검토를 우회하지 않습니다.

## 롤아웃

1. workflow와 Renovate 설정 변경을 PR로 병합하고 새 `verify`가 통과하는지 확인합니다.
2. Renovate App 권한(Dependabot Alerts 읽기, contents/PR/issues 쓰기)과 Dependency Dashboard 생성을 확인합니다.
3. 각 manager가 dependency를 탐지하고, 일반 PR의 월요일 일정·동시성·라벨 및 보안 PR의 즉시 생성·라벨을 실제 실행에서 확인합니다. build-tool catalog 좌표(`dependency-management-gradle-plugin`, `sentry-gradle-plugin`, `ktfmt`)도 Mend 로그에서 추출되는지 확인합니다.
4. Renovate의 보안 PR 생성이 확인된 뒤 Dependabot Security Updates를 끕니다. Dependabot Alerts와 Dependency Graph는 계속 활성화합니다.
5. Ruleset required check 이름을 실제 성공 workflow run의 check context와 대조한 뒤 Ruleset을 활성화합니다. 대상 보호 branch에는 PR, 승인 1명, stale approval 해제, 필수 `verify`를 적용합니다.

## Mend 장애 대응

Maven Central `429` 또는 `external-host-error`가 발생하면 로그와 실패 시각을 기록하고 재실행 결과를 확인합니다. 같은 원인으로 2회 연속 실패한 경우에도 저장소에 cache proxy를 즉시 추가하지 않습니다. Mend 지원 요청과 self-hosted Renovate 전환을 비교해 별도 결정하며, 그 전까지 실패한 manager와 생성되지 않은 보안 PR을 수동으로 확인합니다.

## 운영 기록

dependency PR에는 변경 범위, CI 결과, 취약점의 수정 버전 또는 예외 사유를 남깁니다. 임시 constraint와 Mend 장애에는 CVE/실패 원인, 담당자, 재검토 조건을 issue에 기록합니다. 보안 PR과 major PR은 사람이 런타임 영향과 롤백 가능성을 확인한 뒤 병합합니다.
