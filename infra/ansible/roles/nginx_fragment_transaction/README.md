# nginx_fragment_transaction role design

## Purpose

`nginx_fragment_transaction` is the proposed common boundary for nginx fragment
updates that currently live separately in `nginx_base_config`, `app_bluegreen`,
and `app_stopstart` admin routing. The role is intentionally scaffold-only for
now: it documents the contract and provides a parseable, non-mutating entrypoint
so later PRs can migrate one caller at a time without changing runtime behavior
in this slice.

## DPLAN architecture

- **Data**: callers provide a fragment map that names source/target paths and
  writer commands instead of hard-coding backup loops in each caller.
- **Plan**: the transaction computes pre-state, backups, writer steps, syncs,
  validation, reload requirement, restore actions, and cleanup from that map.
- **Limit**: application lifecycle work remains in the caller; this role owns
  only generated nginx config files and the validate/reload boundary.
- **Adoption**: migrate one caller per PR, starting with `nginx_base_config`,
  then `app_bluegreen`, then `app_stopstart` admin routing.
- **Non-runtime-impact now**: no existing playbook imports this role; the
  scaffold validates and reports a plan only.

## Transaction input: fragment map shape

Callers should pass `nginx_fragment_transaction_fragments` as a list of fragment
records. Each record represents one logical generated file that may be edited in
the deployment source tree and then synchronized to the nginx config volume:

```yaml
nginx_fragment_transaction_fragments:
  - name: backend-upstream
    kind: upstream        # upstream | route | config | legacy
    source: "{{ nginx_generated_source_dir }}/upstreams/backend.conf"
    target: "{{ nginx_generated_target_dir }}/upstreams/backend.conf"
    backup: true          # default true; create/remove <path>.bak around transaction
    required: false       # restore/remove semantics depend on pre-transaction stat
    writer:
      command:
        - python3
        - "{{ deployment_dir }}/update-nginx-config.py"
        - upsert-upstream
        - --path
        - "{{ nginx_generated_source_dir }}/upstreams/backend.conf"
        - --upstream-name
        - "{{ module_cfg.backend_upstream_name }}"
        - --container-name
        - "{{ app_bluegreen_target_container }}"
        - --backend-port
        - "{{ module_cfg.app_port | string }}"
```

Recommended keys:

- `name`: stable label used in logs, backup loops, and migration reports.
- `kind`: small taxonomy for generated `upstreams`, generated `routes`, full
  nginx config files, and the temporary `upstreams/00-managed.conf` migration.
- `source`: writable deployment-tree path, when the caller edits source first.
- `target`: live nginx config-volume path, when the fragment must be promoted.
- `backup`: whether both existing source and target paths get `<path>.bak` files.
- `required`: whether a missing source/target is a caller error instead of a
  normal create-on-success/remove-on-rollback case.
- `writer`: the per-fragment mutation step. The first migration should keep the
  existing `update-nginx-config.py` commands as writer commands.
- `sync`: optional override for source-to-target copy. Default is `source -> target`.
- `changed_register`: optional caller-visible result name when a later role
  implementation needs to expose whether this fragment changed.

The role-level inputs should also include:

```yaml
nginx_fragment_transaction_source_config: "{{ nginx_conf_source_path }}"
nginx_fragment_transaction_target_config: "{{ nginx_conf_target_path }}"
nginx_fragment_transaction_validate_command:
  - docker
  - exec
  - "{{ nginx_container_name }}"
  - nginx
  - -t
nginx_fragment_transaction_reload_command:
  - docker
  - exec
  - "{{ nginx_container_name }}"
  - nginx
  - -s
  - reload
```

## Source/target backup and restore model

The role should capture the pre-transaction state before any writer command:

1. `stat` every declared `source`, `target`, and configured nginx source/target
   config path.
2. Copy existing paths to `<path>.bak`; remove stale backup files for paths that
   were absent before the transaction.
3. Run writer commands only against source paths.
4. Sync changed/generated source fragments to target paths.
5. Restore on any failure using the original `stat` results:
   - if a path existed, copy `<path>.bak` back to the original path;
   - if a path did not exist, remove the newly-created source/target path;
   - restore legacy target `upstreams/00-managed.conf` from its backup when the
     migration helper removed it before validation.
6. After a successful validation/reload, remove all transaction backup files.

This keeps the current semantics from the three callers while removing repeated
backup/restore loops and stale-backup cleanup.

## Validate/reload boundary

`nginx_fragment_transaction` owns nginx validation and reload, not the fragment
writer commands. The transaction should run all writers and source-to-target
syncs first, then execute `nginx_fragment_transaction_validate_command` once.
Only after validation succeeds should it execute
`nginx_fragment_transaction_reload_command`, and only when one of these changed:

- source config bootstrap/include markers;
- any generated upstream or route source file;
- any source-to-target sync;
- removal of legacy `upstreams/00-managed.conf` from the target tree.

On validation or reload failure, the rescue path must restore source/target state,
run validation on the restored config with `failed_when: false`, reload restored
config only when restore validation succeeds, and fail with the restore command
stdout/stderr included in the message.

## Migration path

Migrate callers one at a time; do not rewrite all nginx callers in one PR.

1. **`nginx_base_config`**: first candidate because it already owns full nginx
   seed/promotion. Map backend/admin/actuator upstreams, route fragment, legacy
   `00-managed.conf`, candidate `default.conf`, and live target config into the
   fragment transaction. Keep template rendering and seed writer commands as
   caller-provided writer steps.
2. **`app_bluegreen`**: after base config is stable, move backend and actuator
   upstream updates plus legacy migration into the transaction. Keep container
   start, health checks, active-slot persistence, and old-container cleanup in
   `app_bluegreen` because they are application rollout concerns.
3. **`app_stopstart` / `admin_nginx_route`**: migrate admin upstream and route
   fragments last. Keep the stop-start container lifecycle outside the
   transaction; only admin upstream/route generation, sync, validate, reload, and
   rollback belong here.

The first implementation PR should expose the transaction role behind one caller
only and verify deploy/rollback syntax for `apis`, `admin`, and `batch` before
moving the next caller.

## Current scaffold behavior

The current role entrypoint is intentionally non-mutating. It validates that the
fragment map is list-shaped, builds a visible plan fact, and emits a debug
message. No playbook imports this role yet, so this slice changes no deployment
runtime behavior.
