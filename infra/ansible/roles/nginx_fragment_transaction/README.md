# nginx_fragment_transaction role

## Purpose

`nginx_fragment_transaction` is the common boundary for nginx generated-file
updates shared by `nginx_base_config`, `app_bluegreen`, and `app_stopstart`
admin routing. The role owns only nginx source/target file mutation,
source-to-target promotion, validation, conditional reload, rescue restore, and
backup cleanup. Application lifecycle concerns stay in the caller.

Each caller provides its own file and operation plan; this role keeps the nginx
mutation, validation, reload, and restore behavior consistent across them.

## Contract

Callers provide two explicit lists:

```yaml
nginx_fragment_transaction_files:
  - id: backend-upstream-source
    path: "{{ nginx_generated_source_dir }}/upstreams/backend.conf"
    role: source
    backup: true
    required: false

nginx_fragment_transaction_operations:
  - id: sync-backend-upstream-target
    kind: copy
    src_file: backend-upstream-source
    dest_file: backend-upstream-target
    affects_reload: true
```

### File records

- `id`: unique stable key used by operations and restore facts.
- `path`: absolute remote path.
- `role`: caller-facing label (`source`, `target`, `live-config`, ...).
- `backup`: whether `<path>.bak` is created for existing files and stale backups
  are cleared for absent files. Defaults to `true`.
- `required`: fail before mutation if the file is absent. Defaults to `false`.
- `cleanup_backup_on_success`: remove the backup after the transaction succeeds.
  Defaults to `true`.

Source and target files are intentionally modeled as separate records. The role
never restores a target from a source backup.

### Operation records

Supported `kind` values are intentionally small and auditable:

- `template`: render `template.src` into the declared `dest_file`.
- `command`: run `command.argv`; changed state is parsed from `changed_if`.
- `copy`: copy declared `src_file` to declared `dest_file` with
  `remote_src: true`.
- `file_absent`: remove the declared `file`.

Each operation must declare `affects_reload`. Command operations must use `argv`
not shell strings, and must declare one changed parser:

```yaml
changed_if:
  stdout_json:
    changed: true
```

`stdout_json.changed` parses command stdout as one-line JSON and treats the
operation as changed when the parsed `changed` value matches the declared
boolean. In other words, the published contract key is
`changed_if.stdout_json.changed: true|false`. The legacy
`changed_if.stdout_contains` parser is still supported for older commands, but
new helper calls should emit `{"changed": true|false}` and use
`stdout_json.changed`.

Conditional execution is available through file ids:

- `when_file_exists`: run only when that file exists at operation time.
- `when_file_missing`: run only when that file is absent at operation time.
- `when_file_preexisted`: run only when that file existed before the transaction.

`when_file_missing` is evaluated at operation time, so callers can seed or
create only fragments that are still missing.

## Transaction sequence

1. Validate input shape and operation references.
2. Stat every declared file and store independent pre-state per file id.
3. Back up existing files to `<path>.bak`; clear stale backups for files that
   were absent before the transaction.
4. Execute operations in caller-declared order.
5. Validate nginx with `nginx_fragment_transaction_validate_command`.
6. Aggregate changed operations and reload only when a changed operation has
   `affects_reload: true`.
7. On any operation, validation, or reload failure, restore each file from its
   own pre-state: existing files are copied back from their own backup, and
   files that did not exist before the transaction are removed.
8. Validate restored nginx with `failed_when: false`; reload restored nginx only
   when restored validation succeeds.
9. Fail with caller summary plus restore validation/reload stdout and stderr.
10. On success, remove backup files whose file records keep
    `cleanup_backup_on_success: true`. Callers that need an outer application
    rescue, such as blue-green health/smoke checks, may set it to `false` and
    clean those backups after their own post-transaction checks pass.

The role publishes these facts for caller/reporting use:

- `nginx_fragment_transaction_operation_results`
- `nginx_fragment_transaction_changed`
- `nginx_fragment_transaction_reload_required`
- `nginx_fragment_transaction_file_pre_state`: per-file pre-transaction
  `stat` map keyed by file id. This is published so callers that intentionally
  keep backups after the role succeeds can restore nginx files if a later
  caller-owned step fails.
- `nginx_fragment_transaction_validate_result`
- `nginx_fragment_transaction_reload_result` when reload ran
- `nginx_fragment_transaction_restore_validate_result` on rescue
- `nginx_fragment_transaction_restore_reload_result` on rescue when reload ran

The role defaults `nginx_fragment_transaction_files` and
`nginx_fragment_transaction_operations` to empty lists so accidental direct
imports fail during input validation instead of mutating nginx with an implicit
plan.

## Example caller mapping: `nginx_base_config`

`nginx_base_config` keeps path resolution, directory creation, check-mode
preview, and the live-config existence gate. It then passes the full base nginx
promotion plan to this role:

1. render `default.conf.j2` to `{{ deployment_dir }}/nginx/default.conf`;
2. seed missing backend/admin/actuator upstream source fragments with the
   localhost `127.0.0.1:65535` placeholder;
3. seed the admin route source fragment when missing;
4. sync upstream fragments to the target generated directory;
5. sync the route fragment to target;
6. promote the rendered candidate config to the live target config;
7. validate and conditionally reload through the transaction role.

## Caller boundaries

Keep application lifecycle outside this role:

1. `nginx_base_config` owns path resolution, directory creation, check-mode
   preview, and the live-config existence gate.
2. `app_bluegreen/tasks/run_switch.yml` owns container lifecycle, health checks,
   smoke checks, and slot persistence. It intentionally keeps transaction
   backups until post-switch checks pass. If those application-level checks fail
   after this role has already validated/reloaded nginx, `app_bluegreen` uses
   the published file pre-state to restore each nginx file from its own backup,
   validates/reloads the restored config, then reports both transaction-level
   and post-transaction restore diagnostics.
3. `app_stopstart/tasks/admin_nginx_route.yml` owns admin route intent and
   stop-start container lifecycle; this role only handles backup, sync,
   validation, reload, restore, and cleanup for the nginx files.
