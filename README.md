# BEAT - 소규모 공연을 등록하고 관리할 수 있는 티켓 예매 플랫폼

> 백엔드 Java → Kotlin 마이그레이션 기준과 CI gate 정리는 [`MIGRATION.md`](MIGRATION.md)를 참고하세요.

## BEAT <a href="https://www.beatlive.kr"><img src="https://github.com/user-attachments/assets/49b52b5a-1859-486e-aaf5-e8bee25f64ca" align="left" width="100" alt="BEAT logo"></a>

<a href="https://hits.seeyoufarm.com">
  <img src="https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2FTEAM-BEAT%2FBEAT-SERVER&count_bg=%23FD28C0&title_bg=%230F0F0F&icon=beatport.svg&icon_color=%23E7E7E7&title=hits&edge_flat=false" alt="Hits">
</a>

<br></br>

**📱 BEAT |** [사이트 바로가기](https://www.beatlive.kr/)
</br></br>
**📝 Team Blog |** [BEAT Blog](https://team-beat.tistory.com/) </br>
**📌 Official Account |** [BEAT Instagram](https://www.instagram.com/be_at_beat?igsh=MTJmank3N3phZHYzeA==) </br>
**💌 Email |** [contract@beatlive.kr](mailto:contract@beatlive.kr)

<br></br>
<br></br>

## 💓 Introduction

![intro1](https://github.com/user-attachments/assets/229ca2dd-9fd0-4177-87dd-ce41b4a5186c)
![intro2](https://github.com/user-attachments/assets/7c083b82-6f97-424e-9c70-83706d16e345)

<br></br>

### 🎤 학생 여러분, 아직도 구글폼으로 공연 등록하세요? BEAT로 더 쉽고 빠르게!

![intro3](https://github.com/user-attachments/assets/7a0009a4-a73d-40fb-8976-2b1fa1d7d5f7)
![intro4](https://github.com/user-attachments/assets/7002609e-d4c6-45dd-9fd2-db1547929d1a)

학생 공연 단체들은 대부분 구글폼을 사용하여 공연을 등록하고 관리하고 있습니다. <br>
하지만 예매자의 입금여부를 직접 추적하고 다양한 문의사항을 개인 연락처를 통해 처리해야 하므로 번거로움과 부담을 느끼고 있죠 😭

BEAT는 이러한 문제를 해결하기 위해 탄생했습니다. <br>
**공연 등록, 관리, 예매, 조회까지 한 번에 할 수 있는 통합 플랫폼 BEAT**를 소개합니다! <br>
공연 단체들은 기존의 번거롭고 복잡한 구글폼 대신, BEAT를 사용하여 더 쉽고 편리하게 공연을 관리할 수 있습니다 😁

- **공연 등록**: 등록한 공연 정보를 수정 업데이트 할 수 있으며, 예매자 관리까지 한 곳에서 할 수 있습니다.
- **공연 관리**: 모든 공연 정보를 한 곳에서 관리하고 업데이트할 수 있습니다.
- **공연 예매**: 회원과 비회원 모두 쉽게 사용할 수 있는 예매 시스템으로 관객들이 편리하게 티켓을 구매할 수 있습니다.
- **공연 조회**: 실시간으로 내가 예매한 공연 내역을 조회할 수 있습니다.

BEAT와 함께 효율적이고 체계적으로 공연을 관리해 볼까요? 👏

</br></br>
</br></br>

## ✨ Main Feature

![feature1](https://github.com/user-attachments/assets/3a9cba25-e481-427e-85dd-3231e98a0c30)
![feature2](https://github.com/user-attachments/assets/a2ad201f-80b5-4115-b13a-9f8521f3b984)

</br></br>
</br></br>

## 🧑🏻‍💻 Developers

|                                                                                       이동훈                                                                                       |                                                             황혜린                                                              | 
|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------------:| 
|                              <img width="250" alt="branch" src="https://github.com/user-attachments/assets/bb69bc1c-50d1-44cb-bdcd-febe11fc1a66">                               |     <img width="250" alt="branch" src="https://github.com/user-attachments/assets/fa31601a-48f8-4af8-9690-b8f4f351c04a">     | 
|                                                                   [hoonyworld](https://github.com/hoonyworld)                                                                   |                                 [hyerinhwang-sailin](https://github.com/hyerinhwang-sailin)                                  |
| 티켓 예매 동시성 처리 <br> github action CI 구축 <br> Jenkins multibranch pipeline CD 구축 <br> Jenkins Pipeline에 Slack 연동 <br> Presigned Url(S3) 이미지 서비스 <br> ERD 및 DB 설계 <br> Entity 초기 세팅 | 운영 및 테스트 서버 EC2, RDS 구축 <br> 카카오 소셜 로그인 <br> 인증 / 인가 구현 (Redis) <br> 웹 발신 <br> Swagger 세팅 <br> ERD 및 DB 설계 <br> Entity 초기 세팅 | 

</br></br>
</br></br>

## 🤝 Convention

### 🏡 Git Convention

[Git Convention](https://www.notion.so/jiwoothejay/git-convention-9bee60c3bb0a45f1913616b3e72b87b7)

### 💬 Code Convention

[Code Convention](https://www.notion.so/jiwoothejay/spring-code-convention-15be5fc539a14196b2c360ebfb373856)

### 🌳 Commit Convention

[Commit Convention](https://www.notion.so/jiwoothejay/issue-pr-templates-44f118ed82904febae246518ef150e25)

<br></br>
<br></br>

## 📄 API Specification

<img width="948" alt="api_spec" src="https://github.com/user-attachments/assets/cebac4ae-1104-4a81-b8f6-6ddddfee8e92">

## 📈 ERD

<img width="1742" alt="erd" src="https://github.com/user-attachments/assets/0ac54737-9d8a-4c2d-b46c-1d110414d8eb">

## 🖥️ Tech Stack

### Framework

<img src="https://img.shields.io/badge/Spring_Boot_4-0?style=flat-square&logo=spring-boot&logoColor=white&color=%236DB33F" alt="Spring Boot 4 badge">   <img src="https://img.shields.io/badge/Gradle-0?style=flat-square&logo=gradle&logoColor=white&color=%2302303A" alt="Gradle badge">

#### ORM

<img src="https://img.shields.io/badge/Spring Data JPA-6DB33F?style=flat-square&logo=Databricks&logoColor=white" alt="Spring Data JPA badge">

#### Authorization

<img src="https://img.shields.io/badge/Spring Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security badge">  <img src="https://img.shields.io/badge/JSON Web Tokens-000000?style=flat-square&logo=JSON Web Tokens&logoColor=white" alt="JSON Web Tokens badge">

#### Test

<img src="https://img.shields.io/badge/JUnit5-25A162?style=flat-square&logo=junit5&logoColor=white" alt="JUnit 5 badge">

#### Database

<img src="https://img.shields.io/badge/MySQL-4479A1.svg?style=flat-square&logo=MySQL&logoColor=white" alt="MySQL badge"> <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis badge">

#### AWS

<img src ="https://img.shields.io/badge/AWS EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white" alt="AWS EC2 badge">  <img src ="https://img.shields.io/badge/AWS S3-69A31?style=flat-square&logo=amazons3&logoColor=white" alt="AWS S3 badge">  <img src="https://img.shields.io/badge/AWS RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white" alt="AWS RDS badge">

#### CI/CD

<img src="https://img.shields.io/badge/GitHub%20Actions-0?style=flat-square&logo=GitHub%20Actions&logoColor=white&color=%232088FF" alt="GitHub Actions badge"> <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker badge">

#### Monitoring

<img src="https://img.shields.io/badge/Slack-4A154B?style=flat-square&logo=slack&logoColor=white" alt="Slack badge">

#### Other

<img src="https://img.shields.io/badge/Swagger-6DB33F?style=flat-square&logo=swagger&logoColor=white" alt="Swagger badge">

## 🔨 Architecture

<img src="https://github.com/user-attachments/assets/7b1f3833-f2b0-40fb-970e-e9f8becc9a6d" alt="BEAT server architecture diagram">

현재 논리 Gradle project와 source-directory mapping:

| 구분 | 모듈 | 책임 / 기준서 |
| --- | --- | --- |
| Executable | `:apps:api`, `:apps:admin`, `:apps:batch` | inbound adapter/composition root. Sources remain in deployment-compatible `apis`, `admin`, `batch` directories ([API](apis/README.md), [Admin](admin/README.md), [Batch](batch/README.md)) |
| Application | `:application:frontoffice`, `:application:admin`, `:application:system` | capability-owned use cases, transactions, output ports, query readers |
| Domain | `:domain` | framework-free aggregates, value objects, domain services/events, aggregate repositories ([domain](core/domain/README.md)) |
| Adapter | `:infrastructure` | internal JPA/Redis/external adapters and narrow public bootstrap configuration ([infrastructure](core/infra/README.md)) |
| Cross-cutting | `:support:security`, `:support:observability` | narrow security and observability technical APIs. Sources remain in `gateway`, `observability` directories ([security](gateway/README.md), [observability](observability/README.md)) |

`build-logic`는 Gradle included build이며 application project module은 아닙니다.

### BEAT backend best practice

- `domain`은 Spring/JPA/HTTP를 모르는 순수 모델이며, Aggregate Root가 상태 전이와 불변식을 보호합니다.
- `ApplicationService`는 transaction, 권한, idempotency, repository 호출 순서와 여러 Aggregate 조율을 소유합니다.
- `infra`는 JPA entity와 domain model을 분리하고 mapper로 변환합니다. Domain VO와 persistence `@Embeddable`도 서로 다른 타입입니다.
- `DomainService`는 Entity/VO 하나에 둘 수 없는 순수 정책에만 사용하며 repository와 transaction을 소유하지 않습니다.
- application/domain 오류는 stable code와 의미 기반 type을 소유하고 HTTP status는 실행 모듈 handler가 결정합니다. 기존 `{status, message}` 응답은 client 계약으로 유지합니다. 자세한 기준은 [error handling guide](docs/architecture/error-handling.md)를 따릅니다.
- Kotlin `Result`는 외부 연동의 복구 가능한 실패에만 제한적으로 사용합니다. Domain 규칙과 transactional command의 기본 실패 모델은 예외입니다.
- 같은 transaction에서 지켜야 하는 규칙은 명시적 domain method 호출로 처리합니다. 이벤트는 commit 이후 부수 효과에만 사용합니다.
- DB 제약, lock, idempotency, expand/contract migration과 contract/concurrency test로 애플리케이션 규칙을 보강합니다.

Architecture 정본은 [CQRS multi-module Constitution](docs/architecture/BEAT-SERVER-CQRS-MULTIMODULE-ARCHITECTURE-FINAL.md)입니다.
세부 현행 가이드는 [domain](core/domain/README.md), [infrastructure](core/infra/README.md), [API](apis/README.md),
[security](gateway/README.md), [error handling](docs/architecture/error-handling.md)을 따릅니다.

### Backend migration baseline

Current migration decisions and evidence are maintained in
[BEAT-SERVER-MIGRATION-EXECUTION.md](docs/architecture/BEAT-SERVER-MIGRATION-EXECUTION.md) and `task_artifact.md`.
Domain failures are translated to lane-owned Application failure language before Web adapters map them to HTTP.
[MIGRATION.md](MIGRATION.md) and the ErrorCode inventory are historical pre-target records, not current architecture guidance.

### Environment configuration

Environment-specific secrets and private infrastructure access procedures are managed through SOPS and the team's internal runbook.

## 👥 Contributors

- [BEAT Client Repository](https://github.com/TEAM-BEAT/BEAT-Client)
