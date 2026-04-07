# PR Title
Inventory 기반 SSH 해석 + release published 기반 prod 전체 배포로 전환

## Related issue 🛠

<!-- 관련 이슈 번호를 적어주세요 -->
- closes #(이슈 번호 확인 필요)

## Work Description ✏️

이번 PR은 GitHub Actions 배포 경로의 SSH connection metadata source of truth를 GitHub vars에서 repo/Ansible inventory로 옮기고, prod 배포 정책을 **release published 기반 / 공통 release tag / apis-admin-batch 전체 배포**로 정리한 작업입니다.

### 이번 브랜치에서 완료된 핵심 변경
- dev/prod inventory에 `ssh_host_fingerprint` 추가
- SSH bootstrap을 `setup-ssh-client`로 분리
- caller workflow(`deploy-dev.yml`, `deploy-prod.yml`, `rollback-prod.yml`)에서 SSH host/port/fingerprint 직접 전달 제거
- `resolve-ansible-connection` 액션 + `resolve_ansible_connection.py` 추가
- `_ansible-exec.yml`을 inventory-only SSH resolution 구조로 정리
- resolver fallback과 무관하게 Ansible tooling이 항상 먼저 설치되도록 `_ansible-exec.yml` 시퀀스 수정
- `ansible-secret-aware-verify.yml`에 dev/prod resolver 검증 추가
- `deploy-prod.yml`을 `workflow_dispatch(module+version)`에서 `release.published(shared tag)` 기반으로 전환
- prod는 `github.event.release.tag_name` 하나로 `apis/admin/batch`를 같은 버전으로 build/push/deploy하도록 정리
- `RootRetirementContractTest`를 최신 workflow/contract 기준으로 갱신
- `infra/README.md`를 새 운영 정책(dev=develop 최신 SHA + changed modules / prod=release published + full deploy)에 맞게 갱신

### 핵심 커밋
- `4e376fa0` feat: keep SSH host fingerprints inside the inventory contract
- `2c9e3bc7` refactor: extract SSH bootstrap so deploy tooling can stay focused on Ansible setup
- `0f4c2c7c` chore: prepare caller workflows for inventory-owned SSH metadata
- `2bfb686e` fix: unblock inventory-based SSH resolution against real encrypted inventories
- `a6311a2c` feat: align production deployments with published release tags
- `7f1ed932` fix: keep resolver fallback explicit and align deployment contract tests

### 변경 파일
- `.github/actions/resolve-ansible-connection/action.yml`
- `.github/actions/setup-deploy-tooling/action.yml`
- `.github/actions/setup-ssh-client/action.yml`
- `.github/scripts/resolve_ansible_connection.py`
- `.github/workflows/_ansible-exec.yml`
- `.github/workflows/ansible-secret-aware-verify.yml`
- `.github/workflows/deploy-dev.yml`
- `.github/workflows/deploy-prod.yml`
- `.github/workflows/rollback-prod.yml`
- `infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml`
- `infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml`
- `src/test/java/com/beat/RootRetirementContractTest.java`
- `infra/README.md`
- `.gitignore`

## Trouble Shooting ⚽️

### 해결한 문제
1. **real encrypted inventory에서 resolver가 `ENC[...]`를 반환하던 문제**
   - `ansible-inventory --host`만으로는 `ansible_host` / `ssh_host_fingerprint`가 평문으로 materialize되지 않았습니다.
   - host/group 선택은 그대로 `ansible-inventory`로 두고, 필요한 connection metadata만 임시 `ansible-playbook`로 최소 추출하는 방식으로 보완했습니다.

2. **resolver step과 tooling 설치의 책임이 뒤엉키던 문제**
   - tooling 설치와 resolver 해석을 한 action 경로에 묶어두면 실패 원인이 불명확해질 수 있었습니다.
   - `_ansible-exec.yml`에서 `setup-ansible-tooling`을 별도 필수 step으로 분리하고, resolver action은 해석 전용으로 축소한 뒤 inventory-only 구조로 정리했습니다.

3. **deployment contract test가 현재 구조를 반영하지 못하던 문제**
   - 기존 test는 secret 기반 `ssh_*` caller input, manual prod dispatch, `setup-deploy-tooling` 직접 호출 등을 전제로 하고 있었습니다.
   - 현재 구조(inventory-owned SSH metadata, inventory-only reusable workflow, release published prod policy)에 맞게 contract assertion을 갱신했습니다.

### 남아 있는 외부 blocker
1. **GitHub SSH vars 삭제 불가**
   - 코드 경로는 inventory-only로 정리되었지만, GitHub environment/Actions Variables 삭제는 GitHub 권한으로만 처리할 수 있습니다.
2. **최종 삭제 가능 판정 불가**
   - GitHub 상 Gate 검증(실배포 / vars 삭제 후 재검증)이 끝나야 최종 판정 가능합니다.
3. **GitHub 권한 의존**
   - release publish / 환경 변수 삭제 / prod environment 승인 흐름은 현재 로컬 세션이 아니라 GitHub 권한으로 처리해야 합니다.
4. **실제 release.published 실행 미검증**
   - 로컬 static 검증과 contract test는 통과했지만, 실제 Docker Hub / prod environment와 연결된 GitHub 실행은 아직 확인 전입니다.

## Related ScreenShot 📷

- 없음

## Uncompleted Tasks 😅

- GitHub에서 `ansible-secret-aware-verify.yml` green 확인
- GitHub에서 **release publish** 실행 후 `deploy-prod.yml` 자동 실행 성공 확인
  - apis/admin/batch 3개 모듈 전체가 같은 release tag로 build/push/deploy 되는지 확인
- GitHub에서 `rollback-prod.yml` 수동 검증 1회 수행
- 이후 GitHub SSH vars 삭제 및 post-delete 재검증

### 삭제 대상 GitHub SSH vars
- `DEV_SSH_HOST`
- `DEV_SSH_PORT`
- `DEV_SSH_HOST_FINGERPRINT`
- `PROD_SSH_HOST`
- `PROD_SSH_PORT`
- `PROD_SSH_HOST_FINGERPRINT`

## To Reviewers 📢

- dev 정책은 **develop 최신 SHA 기반 + changed modules deploy** 유지입니다.
- prod 정책은 이번 PR부터 **release published 기반 + 공통 release tag + 3모듈 전체 배포**로 바뀝니다.
- 우선 확인 부탁드리는 포인트는 아래입니다.
  1. `_ansible-exec.yml`의 inventory-only SSH resolution 구조가 의도에 맞는지
  2. resolver 실행 이전에 tooling 설치를 강제한 구조가 안전한지
  3. `deploy-prod.yml`의 `release.published` + shared tag + full-module deploy 정책이 팀 운영 의도와 맞는지
  4. `ansible-secret-aware-verify.yml`의 resolver 검증이 gate 역할로 충분한지

- 머지 후 운영 순서는 아래 기준을 생각하고 있습니다.
  1. verify green 확인
  2. main 기준 release publish
  3. prod 전체 자동 배포 확인
  4. rollback-prod 수동 검증
  5. 재검증 후 GitHub SSH vars 삭제
