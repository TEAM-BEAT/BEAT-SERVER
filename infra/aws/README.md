# BEAT AWS 인프라 (Infrastructure as Code)

CloudFront, Lambda, S3 등 AWS 네이티브 리소스를 정의하는 CloudFormation
템플릿과 소스 코드를 모아둔 디렉토리입니다. Ansible 의 `image_cdn`
역할이 배포를 담당합니다.

---

## 목차

1. [디렉토리 구조](#1-디렉토리-구조)
2. [Phase 1 — Image CDN](#2-phase-1--image-cdn)
   - [아키텍처](#21-아키텍처)
   - [핵심 설계 결정](#22-핵심-설계-결정)
3. [최초 배포 절차](#3-최초-배포-절차)
   - [3-1. 사전 준비](#3-1-사전-준비-1회성)
   - [3-2. 배포 명령](#3-2-배포-명령)
   - [3-3. 배포 후 작업](#3-3-배포-후-작업-1회성)
4. [동작 검증](#4-동작-검증)
5. [모니터링](#5-모니터링)
6. [롤백](#6-롤백)

---

## 1. 디렉토리 구조

```
infra/aws/
├── cloudformation/
│   └── image-cdn.yml                       # Phase 1 — 이미지 전송 스택
├── cloudfront-functions/
│   └── image-cdn-viewer-request.js         # viewer-request 엣지 함수
└── lambda/
    └── image-processing/                   # Sharp 기반 이미지 변환 Lambda
        ├── index.mjs
        └── package.json
```

---

## 2. Phase 1 — Image CDN

### 2-1. 아키텍처

```
                ┌───────────────────────────┐
       viewer ──► CloudFront                │
                │  + viewer-request Function │  ◄─ 경로 정규화
                │   · variant 화이트리스트   │     /<prefix>/<key>?w=480
                │   · Accept 협상            │      → /<prefix>/<key>/format=avif,width=480
                │   · 400 reject             │
                └──────────────┬─────────────┘
                               ▼
                Origin Group  (403/404 발생 시 자동 failover)
                ├─ Primary :  transformed-images S3
                └─ Fallback:  Lambda Function URL
                               │
                               ▼
                Image Processor Lambda (Sharp)
                  1) originals 버킷에서 원본 GET
                  2) Sharp 변환 (resize / format / quality / rotate)
                  3) transformed 버킷에 PUT (이후 캐시 hit)
                  4) 변환 결과를 CloudFront 로 응답
```

### 2-2. 핵심 설계 결정

본 구성은
[`aws-samples/image-optimization`](https://github.com/aws-samples/image-optimization)
패턴을 기반으로 하되, BEAT 특수 요구사항에 맞춰 두 가지를 강화하고
한 가지를 의도적으로 생략했습니다.

> [!NOTE]
> **Origin Shield 는 사용하지 않습니다.** BEAT 트래픽은 한국 단일 region
> 으로 집중되어 multi-region fan-out 효과가 없고, 변환본 S3 저장 패턴으로
> cache miss 자체가 적어 Shield 의 효익이 비용을 초과하지 못합니다.
> 추후 글로벌 트래픽이 생기면 한 줄로 활성화 가능.

| 강화 포인트 | 내용 |
| --- | --- |
| **Variant 화이트리스트 + 400 reject** | 허용되지 않은 width/format/path 는 엣지에서 즉시 거부. 캐시 키 폭증/캐시 버스팅 공격에 본질적 면역. |
| **URI 를 캐시 키로 사용** | viewer-request Function 이 `?w=480` 을 `/format=avif,width=480` 경로 suffix 로 재작성. S3 키 와 캐시 키 가 1:1 매핑되어 Accept 헤더 캐시 오염 부류의 버그를 원천 차단. |

> [!NOTE]
> ADR §4 의 **옵션 B (image-optimization 직접 작성)** 결정에 따른 구현입니다.
> AWS Dynamic Image Transformation 솔루션은 BEAT 가 사용하지 않는
> Rekognition / smart crop / request signing 등이 포함되어 over-spec 으로 판단되어
> 채택하지 않았습니다.

---

## 3. 최초 배포 절차

### 3-1. 사전 준비 (1회성)

#### ① Lambda 아티팩트 보관용 S3 버킷 생성

```bash
aws s3 mb s3://beat-lambda-artifacts-dev  --region ap-northeast-2
aws s3 mb s3://beat-lambda-artifacts-prod --region ap-northeast-2
```

#### ② SOPS 시크릿에 알람 이메일 추가

각 환경의 `infra/ansible/inventories/{dev,prod}/group_vars/all/secrets.sops.yml`
파일을 `sops` 로 열어 다음 키를 추가합니다.

```yaml
image_cdn_alarm_email: alerts@example.com
```

#### ③ (선택) ACM 인증서 + Route 53 — 커스텀 도메인 사용 시

`image_cdn_custom_domain` 을 설정할 때만 필요합니다.

- ACM 인증서는 **반드시 us-east-1 리전** 에 있어야 합니다. CloudFront 가 이 리전만 인식합니다.
- 발급된 인증서 ARN 을 SOPS 시크릿에 추가:

  ```yaml
  image_cdn_acm_cert_arn: "arn:aws:acm:us-east-1:<account>:certificate/<id>"
  ```

#### ④ 빌드 호스트 요구사항

플레이북 실행 호스트(GHA runner 또는 로컬) 에 다음이 설치되어 있어야 합니다.

- Node.js 22 이상 (`npm install` 용)
- `zip` CLI (Lambda 패키지 생성용)

---

### 3-2. 배포 명령

```bash
# dev 먼저
ansible-playbook \
  -i infra/ansible/inventories/dev \
  infra/ansible/playbooks/image_cdn.yml \
  -e deploy_environment=dev

# dev 검증 통과 후 prod
ansible-playbook \
  -i infra/ansible/inventories/prod \
  infra/ansible/playbooks/image_cdn.yml \
  -e deploy_environment=prod
```

#### 역할 내부 동작 순서

1. `npm install --omit=dev --os=linux --cpu=arm64` 로 Linux/arm64 용
   `sharp` 바이너리를 포함한 Lambda 패키지 빌드
2. 패키지 내용의 SHA 해시를 산출하여
   `s3://beat-lambda-artifacts-<env>/image-cdn/<hash>/image-processing.zip`
   경로로 업로드
3. CloudFront Function 소스 (`image-cdn-viewer-request.js`) 를 읽어 들여
   CloudFormation 파라미터로 전달
4. 해시 기반 키로 CloudFormation 스택 갱신 — 코드가 실제로 바뀐 경우에만
   Lambda 가 업데이트됨 (idempotent)

---

### 3-3. 배포 후 작업 (1회성)

#### ① 커스텀 도메인 Route 53 레코드

`image_cdn_custom_domain` 을 설정한 경우, 스택 출력값 `DistributionDomainName`
을 확인한 뒤 Route 53 에 alias (또는 CNAME) 레코드를 추가합니다.

```
cdn.beatlive.kr  →  d{xxxxxxx}.cloudfront.net
```

#### ② SNS 구독 확인

AWS 가 발송하는 SNS 구독 확인 메일에서 링크를 클릭해야 알람이
정상 전송됩니다.

---

## 4. 동작 검증

스택 출력의 `DistributionDomainName` 또는 커스텀 도메인을 사용해 확인합니다.

### 정상 케이스

```bash
# 첫 요청: Cache Miss → Lambda 가 변환본을 S3 에 저장
curl -I "https://<도메인>/poster/<existing-key>?w=480"
# 기대: HTTP/2 200, x-cache: Miss from cloudfront

# 두 번째 요청: Cache Hit
curl -I "https://<도메인>/poster/<existing-key>?w=480"
# 기대: HTTP/2 200, x-cache: Hit from cloudfront
```

### Reject 케이스 (보안 검증)

```bash
# 허용되지 않은 width
curl -I "https://<도메인>/poster/<existing-key>?w=481"
# 기대: HTTP/2 400  (viewer-request Function 거부)

# 허용되지 않은 path prefix
curl -I "https://<도메인>/etc/passwd"
# 기대: HTTP/2 400  (path 화이트리스트 위반)
```

> [!IMPORTANT]
> 위 400 응답이 실제로 떨어져야 캐시 버스팅 방어가 동작하고 있다는 증거입니다.
> 200 으로 돌아오면 Function 이 distribution 에 attach 되지 않았거나 화이트리스트
> 값이 잘못된 것이니 즉시 점검 필요.

---

## 5. 모니터링

CloudWatch 알람으로 다음 3가지가 자동 설정됩니다.

| 알람 | 임계값 | 대상 |
| --- | --- | --- |
| Lambda Errors | 5분 합계 > 5건 | `beat-image-cdn-processor-<env>` |
| CloudFront 5xx | 2회 연속 평균 > 0.5% | distribution |
| Cache Hit Rate | 3회 연속 평균 < 90% | distribution |

> [!NOTE]
> **비용 알람은 AWS Budgets 로 별도 관리합니다.** `AWS/Billing` 메트릭은
> us-east-1 리전에서만 publish 되므로 본 스택(ap-northeast-2)의
> CloudWatch Alarm 으로는 동작하지 않습니다. AWS Budgets 콘솔에서
> 월별 비용/사용량 예산을 SNS 또는 이메일로 받도록 설정하세요.

### Grafana 대시보드

Grafana 대시보드의 코드 관리 패턴이 아직 도입 전이라, Phase 1 단계에서는
Grafana Cloud UI 에서 CloudWatch 데이터소스를 직접 추가해 사용합니다.
Phase 4 에서 JSON-as-code 패턴 정착 시 코드화 예정입니다.

추천 패널:

- **Lambda** (`AWS/Lambda`, function = `beat-image-cdn-processor-<env>`)
  Errors / Duration / Throttles / ConcurrentExecutions
- **CloudFront** (`AWS/CloudFront`, distribution = 스택 출력값)
  Requests / BytesDownloaded / CacheHitRate / 5xxErrorRate

---

## 6. 롤백

```bash
aws cloudformation delete-stack \
  --stack-name beat-image-cdn-<env> \
  --region ap-northeast-2
```

> [!NOTE]
> **transformed-images 버킷** 은 `DeletionPolicy: Retain` 으로 설정되어
> 스택 삭제 후에도 남습니다. 깨끗하게 재배포하려면 수동으로 비운 뒤
> 재배포하세요. **originals 버킷은 본 스택이 절대 수정하지 않습니다.**
