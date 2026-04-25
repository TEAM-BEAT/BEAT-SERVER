# nginx_fragment_transaction role

## Purpose

`nginx_fragment_transaction` is the common boundary for nginx generated-file
updates that used to be hand-rolled in `nginx_base_config`, `app_bluegreen`, and
`app_stopstart` admin routing. The role owns only nginx source/target file
mutation, source-to-target promotion, validation, conditional reload, rescue
restore, and backup cleanup. Application lifecycle concerns stay in the caller.

PR8-A wires this role into `nginx_base_config` only. `app_bluegreen` and
`app_stopstart` admin routing remain separate follow-up migrations.

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
- `role`: caller-facing label (`source`, `target`, `legacy`, `live-config`, ...).
- `backup`: whether `<path>.bak` is created for existing files and stale backups
  are cleared for absent files. Defaults to `true`.
- `required`: fail before mutation if the file is absent. Defaults to `false`.
- `cleanup_backup_on_success`: remove the backup after successful validation and
  reload. Defaults to `true`.

Source and target files are intentionally modeled as separate records. The role
never restores a target from a source backup.

### Operation records

Supported `kind` values are intentionally small and auditable:

- `template`: render a controller template into a declared destination file.
- `command`: run an argv command; changed state is parsed from `changed_if`.
- `copy`: copy one declared file to another with `remote_src: true`.
- `file_absent`: remove one declared file.

Each operation must declare `affects_reload`. Command operations must use `argv`
not shell strings, and must declare `changed_if.stdout_contains`.

Conditional execution is available through file ids:

- `when_file_exists`: run only when that file exists at operation time.
- `when_file_missing`: run only when that file is absent at operation time.
- `when_file_preexisted`: run only when that file existed before the transaction.

`when_file_missing` is evaluated at operation time, so callers can split legacy
upstreams first and then seed only fragments that are still missing.

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
10. On success, remove all transaction backup files.

The role publishes these facts for caller/reporting use:

- `nginx_fragment_transaction_operation_results`
- `nginx_fragment_transaction_changed`
- `nginx_fragment_transaction_reload_required`
- `nginx_fragment_transaction_validate_result`
- `nginx_fragment_transaction_reload_result` when reload ran
- `nginx_fragment_transaction_restore_validate_result` on rescue
- `nginx_fragment_transaction_restore_reload_result` on rescue when reload ran

## `nginx_base_config` mapping

`nginx_base_config` keeps path resolution, directory creation, check-mode
preview, and the live-config existence gate. It then passes the full base nginx
promotion plan to this role:

1. render `default.conf.j2` to `{{ deployment_dir }}/nginx/default.conf`;
2. split legacy `upstreams/00-managed.conf` into backend/admin/actuator
   fragments with `--require-all --skip-existing` when legacy source exists;
3. remove legacy source when it existed before the transaction;
4. seed missing backend/admin/actuator upstream source fragments with the
   localhost `127.0.0.1:65535` placeholder;
5. seed the admin route source fragment when missing;
6. sync upstream fragments to the target generated directory;
7. remove legacy target before nginx validation when it existed before the
   transaction;
8. sync the route fragment to target;
9. promote the rendered candidate config to the live target config;
10. validate and conditionally reload through the transaction role.

## Migration path

Migrate callers one at a time:

1. `nginx_base_config` in PR8-A.
2. `app_stopstart/tasks/admin_nginx_route.yml` after the base transaction is
   proven.
3. `app_bluegreen/tasks/run_switch.yml` last, because it also owns container
   lifecycle, health checks, public smoke checks, old-container cleanup, and
   active-slot persistence.
