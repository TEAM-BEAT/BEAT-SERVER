# Phase 1 — Image CDN 배포 노트 (운영 컨텍스트)

본 문서는 BEAT 의 Image CDN(Phase 1) 첫 배포 과정에서 발견된 함정과
해결 방식을 시간순으로 정리한 운영 노트입니다. 향후 prod 배포, 다른
환경 재현, 다른 작업자(또는 다른 AI 에이전트)가 맥락을 빠르게 파악할 수
있도록 작성되었습니다.

상위 의사결정 기록은 ADR (Issue #510) 본문을, 코드 산출물은
[PR #515](https://github.com/TEAM-BEAT/BEAT-SERVER/pull/515) 를 참조.

---

## 1. 무엇을 만들었나

| 영역 | 내용 |
| --- | --- |
| IaC | `infra/aws/cloudformation/image-cdn.yml` (단일 CFN 템플릿) |
| Lambda 변환기 | `infra/aws/lambda/image-processing/index.mjs` (Sharp + S3) |
| CloudFront Function | `infra/aws/cloudfront-functions/image-cdn-viewer-request.js` |
| 배포 자동화 | `infra/ansible/roles/image_cdn/` + `playbooks/image_cdn.yml` |
| 환경 변수 | `inventories/{dev,prod}/group_vars/all/image_cdn.yml` |
| 시크릿 | `inventories/{dev,prod}/group_vars/all/secrets.sops.yml` 에
  `image_cdn_alarm_email` 추가 |

### 아키텍처 한 줄 요약

> CloudFront → viewer-request Function (화이트리스트 + Accept 협상 + URI 정규화)
> → Origin Group (Primary: transformed-images S3 / Fallback: Lambda Function URL)
> → Lambda(Sharp)가 originals 에서 GET → 변환 → transformed 에 PUT → 응답

자세한 그림과 의사결정 근거는 `infra/aws/README.md` 와 ADR (#510) 참조.

---

## 2. 운영 환경 (확정 사실)

| 항목 | 값 |
| --- | --- |
| dev AWS 계정 | `169634835710` (profile: `beat-dev`) |
| prod AWS 계정 | `637423615550` (profile: `beat-prod`) |
| Region | `ap-northeast-2` |
| 도메인 | `beatlive.kr` (Route 53 hosted zone 보유) |
| SOPS age 키 | 운영자 로컬 `~/Library/Application Support/sops/age/keys.txt` |
| BEAT 표준 배포 패턴 | GitHub Actions + Ansible + SOPS (EC2 호스트 대상) |

> [!IMPORTANT]
> Image CDN role 은 **BEAT 에서 최초로 AWS API(CloudFormation, S3 PutObject)를
> 호출하는 ansible 작업**입니다. 기존 워크플로우는 EC2 SSH 기반이라
> AWS API 분기 패턴이 없었습니다. 본 role 은 환경변수(`AWS_PROFILE` /
> `AWS_ACCESS_KEY_ID` 등)로 credentials 를 받도록 설계.

### dev 첫 배포 결과 (2026-05-23/24)

| 항목 | 값 |
| --- | --- |
| Distribution domain | `d3oubuynagl140.cloudfront.net` |
| Distribution ID | `E118XVS2UFAI7E` |
| Transformed bucket | `beat-image-cdn-transformed-dev` |
| SNS alarm topic | `arn:aws:sns:ap-northeast-2:169634835710:beat-image-cdn-alarms-dev` |
| Lambda LogGroup | `/aws/lambda/beat-image-cdn-processor-dev` (14일 retention) |

---

## 3. 배포 중 발견된 함정과 해결 (시간순)

배포는 dev 에서 8번의 사이클을 거쳐 통과되었습니다. 각 함정은 commit
로 코드/설정에 반영되어 있어 prod 배포 시 동일 사이클을 반복할 필요는
없습니다.

### 함정 ①: ansible role 못 찾음 (`role 'image_cdn' was not found`)

**원인**: `ansible.cfg` 의 `roles_path = roles` 는 **상대경로**.
`ansible-playbook` 을 BEAT-SERVER 루트에서 호출하면 `<repo>/roles` 로
해석되어 못 찾음.

**해결**: README 의 실행 가이드를 `cd infra/ansible` 으로 시작하도록
수정. 기존 BEAT 의 모든 ansible 호출이 동일 패턴.

---

### 함정 ②: `boto3` / `botocore` 미설치

**원인**: pipx 로 설치한 ansible venv 에 `amazon.aws.cloudformation` /
`amazon.aws.s3_object` 모듈이 의존하는 boto3 가 없음.

**해결**: `pipx inject ansible boto3 botocore`. 운영자 로컬 환경 변경
1회. 향후 GHA workflow 에서는 `setup-ansible-tooling` 액션이 동일
의존성을 install 하도록 보강 필요(별도 PR).

---

### 함정 ③: 경로가 `infra/infra/aws/...` 로 깨짐

**원인**: tasks/main.yml 의 `image_cdn_repo_root` 산정이 `dirname` 을
2회만 적용 → `playbook_dir` (`.../infra/ansible/playbooks`) 에서 두 번
올라가서 `.../infra` 에서 멈춤. 한 번 더 올라가야 BEAT-SERVER 루트.

**해결 (commit `b7377663`)**: `dirname` 3회로 수정.

---

### 함정 ④: `AWS::CloudFront::CachePolicy` 의 Comment 128자 초과

**원인**: CloudFront CachePolicy 의 `Comment` 는 128자 제한. 첫 작성에서
146자 → CFN 이 `InvalidRequest` 로 CREATE_FAILED 반환 → 전체 스택 rollback.

**해결 (commit `f02906fd`)**: 코멘트를 86자로 단축. 다른 CloudFront
리소스(OAC, ResponseHeadersPolicy, Function)의 Comment 도 128자 이내
유지 필요.

---

### 함정 ⑤: 2번째 `AWS::Lambda::Permission` 의 `FunctionUrlAuthType` 오용

**원인**: AWS docs 와 image-optimization CDK 가 권장하는 **두 개의
Permission**(`lambda:InvokeFunctionUrl` + `lambda:InvokeFunction`) 중,
2번째(`InvokeFunction`)에 잘못 `FunctionUrlAuthType: AWS_IAM` 을 추가.
이 속성은 **`InvokeFunctionUrl` action 에만 유효** → `InvalidRequest`.

**해결 (commit `faba889d`)**: 2번째 permission 에서 `FunctionUrlAuthType`
제거. 첫 번째 permission(`InvokeFunctionUrl`)에만 유지.

---

### 함정 ⑥: Sharp 의 `linux-arm64` native binding 이 zip 에 미포함

**원인**: Sharp 0.33 부터 platform-specific binary 가 **별도 optional
package** (`@img/sharp-linux-arm64`) 로 분리됨. `--os=linux --cpu=arm64`
flag 만으로는 host platform(macOS) 의 binary 만 받음. 또한 Lambda 의
Amazon Linux 는 glibc 변종이라 명시 필요.

**해결 (commit `8895e329`)**: ansible task 의 `npm install` 명령에
`--include=optional --libc=glibc` 추가. 결과적으로 `npm install
--omit=dev --include=optional --os=linux --cpu=arm64 --libc=glibc`.

이 변경은 npm 10.4+ 의 flag 들. 운영자 로컬 npm 11.x 에서 동작 확인.
CI 에서는 동일 npm 버전 보장 필요.

---

### 함정 ⑦: S3 `GetObject AccessDenied` (같은 account 안에서도)

**가장 진단이 까다로웠던 함정.**

**현상**: Lambda 가 `beat-dev-bucket` 의 객체를 GetObject 시도 →
`AccessDenied`. Lambda role 의 IAM policy 는 `s3:GetObject` 명시적
Allow. bucket policy 는 `Principal: "*"` + `Allow GetObject`. 모든 것
이 허용해도 deny.

**진단**:
- bucket 소유 account = `169634835710` (beat-dev)
- Pre-signed URL 발급용 IAM user 소속 = 같은 account
- Lambda role 소속 = 같은 account
- 즉 cross-account 이슈 아님

**진짜 원인**: BEAT 의 백엔드(`infra/src/.../S3InfraConfig.java`)가 발급한
Pre-signed URL 로 클라이언트가 PUT 한 객체들의 **ACL 이 private**
(default) 이고 **owner 가 PUT 발급한 IAM user**. S3 의 legacy ACL 규칙상
같은 account 안이라도 ACL 의 private 가 IAM policy 의 Allow 를 차단할
수 있는 corner case 발생.

**해결**: `beat-dev-bucket` 의 ObjectOwnership 을 `BucketOwnerEnforced`
로 변경. 이는:
- 모든 ACL 비활성화
- bucket owner 가 모든 object 의 owner
- access 통제는 IAM/bucket policy 만으로
- 기존 객체에도 즉시 적용
- AWS 2023 이후 신규 버킷의 기본값(사실상 표준)

```bash
AWS_PROFILE=beat-dev aws s3api put-bucket-acl --bucket beat-dev-bucket --acl private
AWS_PROFILE=beat-dev aws s3api put-bucket-ownership-controls \
  --bucket beat-dev-bucket \
  --ownership-controls 'Rules=[{ObjectOwnership=BucketOwnerEnforced}]'
```

> [!WARNING]
> **prod 배포 전 `beat-prod-bucket` 도 동일 변경 필요**합니다. 단 prod
> 운영 정책에 ACL 의존하는 다른 시스템이 있는지 확인 후 진행.
> BEAT 의 Spring Boot 코드(`S3InfraConfig`)는 ACL 을 명시적으로
> 다루지 않으므로 영향이 거의 없을 것으로 추정되나, 실 운영 측면에서
> 변경 전 backup 권장.

---

## 4. 검증 결과 (dev)

```bash
# 원본 103,330 bytes 의 poster 이미지에 대해:
# JPEG (default)
curl -s ".../poster/<key>?w=480" | wc -c       # 13,857 bytes (87% 절감)

# AVIF 협상
curl -sH "Accept: image/avif" ".../poster/<key>?w=480" | wc -c   # 7,944 bytes (92.3% 절감)

# WebP 협상
curl -sH "Accept: image/webp" ".../poster/<key>?w=480" | wc -c   # 10,404 bytes (89.9% 절감)

# 화이트리스트 위반 reject
curl -sI ".../poster/<key>?w=481"       # HTTP/2 400 Bad Request
curl -sI ".../etc/passwd"               # HTTP/2 400 Bad Request

# 두 번째 호출 cache hit
curl -sI ".../poster/<key>?w=480"       # x-cache: Hit from cloudfront, Server-Timing 사라짐
```

S3 transformed 버킷에 자동 저장:
```
beat-image-cdn-transformed-dev/poster/<key>/format=avif,width=480   (7,944 B)
beat-image-cdn-transformed-dev/poster/<key>/format=jpeg,width=480   (13,857 B)
beat-image-cdn-transformed-dev/poster/<key>/format=webp,width=480   (10,404 B)
```

Lambda 콜드 스타트:
- Init Duration: 418 ms (Sharp 로드)
- Max Memory Used: 119 MB (할당 1500 MB 중 약 8%, 충분한 여유)
- Total Duration (cache miss 시): ~162 ms (S3 download 132 ms + Sharp transform 30 ms)

---

## 5. prod 배포 전 체크리스트

| 항목 | 비고 |
| --- | --- |
| ⬜ `beat-prod-bucket` 도 `BucketOwnerEnforced` 변경 | 함정 ⑦ 참조. **운영 영향 확인 후** 실행 |
| ⬜ ACM 인증서 us-east-1 에 발급 (`cdn.beatlive.kr`) | 커스텀 도메인 사용 시 |
| ⬜ SOPS prod 시크릿에 `image_cdn_acm_cert_arn` 추가 | 위와 연동 |
| ⬜ Route 53 에 DNS validation CNAME 추가 | ACM 발급 검증용 |
| ⬜ prod ansible-playbook 실행 | dev 와 동일 명령, profile 만 변경 |
| ⬜ Route 53 에 `cdn.beatlive.kr` alias (→ CloudFront) | 배포 후 |
| ⬜ SNS 구독 이메일 컨펌 (`beatlebeatle.official@gmail.com`) | dev/prod 각각 |

---

## 6. 후속 작업 (별도 PR scope)

| Phase | 이슈 | 내용 |
| --- | --- | --- |
| GHA workflow | (신규) | `deploy-image-cdn-{dev,prod}.yml` — OIDC role 로 manual ansible 대체. 본격 운영 시 표준 배포 경로 |
| Phase 2 | #512 | BE Pre-warming worker + DB URL 5개 컬럼 prefix 일괄 치환 마이그레이션 |
| Phase 3 | #513 | FE: `cdn.beatlive.kr` 일괄 치환, `<img srcset>` 반응형 적용 |
| Phase 4 | #514 | Lighthouse/WebPageTest 측정, Grafana 대시보드 코드화, Pre-warming 95% hit ratio 검증 |

---

## 7. 변경 commit 이력 (PR #515 기준)

```
8895e329  fix: Sharp linux-arm64 native binding 이 zip 에 포함되도록 npm install flag 보강
faba889d  fix: 2번째 Lambda Permission 에서 FunctionUrlAuthType 제거
f02906fd  fix: CachePolicy Comment 를 128자 제한 안으로 단축
b7377663  fix: image_cdn role 의 repo_root 산정을 dirname 3회로 수정
e2fd1d41  refactor: image_cdn AWS credentials 를 환경변수 기반으로 변경
cb8a9576  chore: image CDN 알람 수신 이메일 SOPS 시크릿 추가 (dev/prod)
188757df  chore: image CDN Origin Shield 비활성화 (한국 단일 region 패턴에서 비용 대비 효과 미미)
b8611458  feat: image CDN 인프라 구축 (CloudFront + Lambda Function URL + S3 변환본 캐싱)
```

추가로 **AWS 측 manual 작업** (코드 commit 외):
- `beat-lambda-artifacts-{dev,prod}` 2개 S3 버킷 신규 생성 (Lambda zip 보관용)
- `beat-dev-bucket` 의 ObjectOwnership 을 `BucketOwnerEnforced` 로 변경
- (prod 시 동일 처리 필요)
