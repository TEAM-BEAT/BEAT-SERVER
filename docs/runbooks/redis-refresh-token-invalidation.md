# Redis refresh-token invalidation runbook

Use this during the JWT signing-key migration deployment. The application keeps Redis-backed refresh-token revocation; this runbook only removes refresh tokens issued before the migration so legacy refresh tokens cannot extend sessions.

## Preconditions

- New application version is deployed with decoded Base64 JWT signing and `tokenType` validation.
- `jwt.legacy-access-token-verify-until` is set to deployment time plus access-token TTL plus clock skew.
- `jwt.secret` is the Base64 text form of the current raw JWT secret bytes.
- `jwt.legacy-secret` is explicitly injected with the previous raw JWT secret until the cutoff. The application fails
  fast if legacy fallback is enabled without this value.
- Redis key patterns are verified in staging. Do not run broad `KEYS *` in production.

## Dry run

```bash
redis-cli --scan --pattern 'refreshToken*' | tee /tmp/refresh-token-keys.txt
wc -l /tmp/refresh-token-keys.txt
head -20 /tmp/refresh-token-keys.txt
```

Spring Data Redis stores the entity namespace as `refreshToken`; because `RefreshToken.refreshToken` is indexed, include matching secondary-index keys that staging confirms belong to this aggregate.

## Delete

Prefer batched `SCAN` + `UNLINK`/`DEL` with the verified namespace only:

```bash
redis-cli --scan --pattern 'refreshToken*' \
  | xargs -r -n 500 redis-cli UNLINK
```

If `UNLINK` is unavailable, use `DEL` in the same batched shape.

## Verify

```bash
redis-cli --scan --pattern 'refreshToken*' | tee /tmp/refresh-token-remaining.txt
wc -l /tmp/refresh-token-remaining.txt
```

Expected remaining count: `0` for verified refresh-token entity/index keys. Record the before/delete/after counts in the deployment log.

## Smoke checks

1. Existing refresh token fails refresh because Redis lookup is missing.
2. New login issues new access/refresh tokens.
3. New refresh token is stored and can refresh access tokens.
4. Sending a refresh token as `Authorization: Bearer` is rejected.
