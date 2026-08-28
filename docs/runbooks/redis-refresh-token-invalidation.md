# Redis 전체 refresh-token 무효화 런북

JWT signing key 교체처럼 **모든 기존 세션을 종료해야 할 때만** 사용합니다. 현재 저장 구조는 refresh token 원문을 secondary-index key에 포함하므로 key를 터미널, 파일, CI log, shell argument로 반출하지 않습니다.

이 절차는 “구버전 토큰만” 선별하지 않습니다. 실행 시점의 `refreshToken:*` hash/index key를 모두 제거하므로 점검 중 새로 발급된 토큰도 무효화됩니다.

## Preconditions

- 로그인/재발급 트래픽을 maintenance mode로 차단하고 in-flight 요청이 끝날 때까지 기다립니다.
- 현재 Spring Data Redis keyspace는 exact key `refreshToken`과 hash/index prefix `refreshToken:*`를 함께 사용합니다. staging에서 이 구조와 application Redis DB 번호를 확인하고 다르면 중단합니다. production에서 `KEYS *`를 사용하지 않습니다.
- 대상 host와 Redis container가 맞는지 확인하고, 삭제 전 Redis backup/복구 정책을 확인합니다.
- 실행자 terminal/CI의 command tracing(`set -x`)과 session recording에 secret key가 출력되지 않도록 합니다.

## Count without exporting keys

Redis container 안에서 server-side `SCAN`으로 개수만 반환합니다. key 값은 client로 반환하지 않습니다. 현재 application은 별도 database 설정이 없어 DB `0`을 사용합니다. 비밀번호는 command line이나 shell history에 쓰지 않고 silent prompt로 `REDISCLI_AUTH`에 전달합니다.

```bash
read -rsp 'Redis password: ' REDISCLI_AUTH && printf '\n'
export REDISCLI_AUTH
REDIS_DB=0

docker exec -e REDISCLI_AUTH redis redis-cli -n "$REDIS_DB" --raw EVAL \
  'local c="0"; local n=redis.call("EXISTS","refreshToken"); repeat local r=redis.call("SCAN",c,"MATCH","refreshToken:*","COUNT",500); c=r[1]; n=n+#r[2] until c=="0"; return n' \
  0
```

## Delete

maintenance mode가 유지되는 동안 Redis 내부에서 같은 namespace만 `UNLINK`합니다. 반환값은 삭제를 요청한 key 개수입니다. 데이터가 매우 많으면 단일 Lua 실행이 Redis를 오래 점유할 수 있으므로 staging에서 실행 시간을 측정하고 별도 batched 운영 도구를 준비합니다.

```bash
docker exec -e REDISCLI_AUTH redis redis-cli -n "$REDIS_DB" --raw EVAL \
  'local c="0"; local n=0; if redis.call("EXISTS","refreshToken")==1 then redis.call("UNLINK","refreshToken"); n=n+1 end; repeat local r=redis.call("SCAN",c,"MATCH","refreshToken:*","COUNT",500); c=r[1]; for _,k in ipairs(r[2]) do redis.call("UNLINK",k); n=n+1 end until c=="0"; return n' \
  0
```

## Verify and resume

1. 위 count 명령의 결과가 `0`인지 확인합니다. `SCAN` 중 삭제로 잔여 key가 있으면 maintenance mode에서 delete/count를 반복합니다.
2. 새 버전을 배포하고 신규 로그인을 한 번 수행합니다.
3. 새 refresh token으로 access token 재발급이 되는지 확인합니다.
4. 교체 전 refresh token과 access token을 refresh endpoint에 넣었을 때 거부되는지 확인합니다.
5. `unset REDISCLI_AUTH REDIS_DB`로 local environment를 정리한 뒤 maintenance mode를 해제합니다.

배포 기록에는 실행 시각, 환경, 실행자, before/delete/after **개수만** 남기며 token/key 원문은 남기지 않습니다.
