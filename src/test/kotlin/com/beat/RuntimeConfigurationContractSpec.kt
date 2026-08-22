package com.beat

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

class RuntimeConfigurationContractSpec : FunSpec() {
    init {
            test("persistenceProfileKeepsProdSqlLoggingOffWhileDevSqlLoggingStaysExplicitOptIn") {
            val persistence = read("core/infra/src/main/resources/application-persistence.yml")
            val baseSection = persistence.substring(0, persistence.indexOf("\n---"))
            val prodSection = sectionAfter(persistence, "on-profile: prod")
            val devSection = sectionAfter(persistence, "on-profile: dev")

            (devSection.contains("show-sql: true")) shouldBe true
            (baseSection.contains("format_sql: true")) shouldBe true
            (prodSection.contains("show-sql: false")) shouldBe true
            (prodSection.contains("format_sql: false")) shouldBe true
            (prodSection.contains("org.hibernate.SQL: WARN")) shouldBe true
            (prodSection.contains("org.hibernate.orm.jdbc.bind: WARN")) shouldBe true
            (prodSection.contains("org.hibernate.SQL: DEBUG")) shouldBe false
            (prodSection.contains("org.hibernate.SQL: TRACE")) shouldBe false
            (prodSection.contains("org.hibernate.orm.jdbc.bind: DEBUG")) shouldBe false
            (prodSection.contains("org.hibernate.orm.jdbc.bind: TRACE")) shouldBe false
        }


            test("nginxBaseConfigOwnsConservativeScannerBlockContract") {
            val defaultConfTemplate = read("infra/ansible/roles/nginx_base_config/templates/default.conf.j2")
            val defaults = read("infra/ansible/roles/nginx_base_config/defaults/main.yml")
            val tasks = read("infra/ansible/roles/nginx_base_config/tasks/main.yml")
            val infraReadme = read("core/infra/README.md")
            val httpServer = sectionBetween(defaultConfTemplate, "server {\n    listen 80;", "\n}\n\nserver {\n    listen 443 ssl;")
            val httpsServer = defaultConfTemplate.substring(defaultConfTemplate.indexOf("server {\n    listen 443 ssl;"))

            (defaultConfTemplate.contains("macro render_scanner_policy()")) shouldBe true
            (countOccurrences(defaultConfTemplate, "{{ render_scanner_policy() }}")) shouldBe (2)
            assertBefore(httpServer, "location ^~ /.well-known/acme-challenge/", "{{ render_scanner_policy() }}")
            assertBefore(httpServer, "{{ render_scanner_policy() }}", "return 301 https://\$host\$request_uri;")
            assertBefore(httpsServer, "{{ render_scanner_policy() }}", "BEAT MANAGED GENERATED ROUTE INCLUDES")
            (defaultConfTemplate.contains("location = {{ path }}")) shouldBe true
            (defaultConfTemplate.contains("location ^~ {{ prefix }}")) shouldBe true
            (defaultConfTemplate.contains("location ~ \"^/\\.env(?:\\.[A-Za-z0-9_-]{1,32})?\$\"")) shouldBe true
            (defaultConfTemplate.contains("location ~ ^/\\.env")) shouldBe false
            (defaultConfTemplate.contains(".*\\.php")) shouldBe false
            (defaultConfTemplate.contains("limit_req ")) shouldBe false
            (defaultConfTemplate.contains("limit_req_zone")) shouldBe false

            (defaults.contains("nginx_base_config_scanner_block_enabled: true")) shouldBe true
            (defaults.contains("nginx_base_config_scanner_block_status: 404")) shouldBe true
            (defaults.contains("- /.env")) shouldBe true
            (defaults.contains("- /.git/config")) shouldBe true
            (defaults.contains("- /wp-login.php")) shouldBe true
            (defaults.contains("- /xmlrpc.php")) shouldBe true
            (defaults.contains("- /index.php")) shouldBe true
            (defaults.contains("- /phpinfo.php")) shouldBe true
            (defaults.contains("- /info.php")) shouldBe true
            (defaults.contains("- /wordpress/")) shouldBe true
            (defaults.contains("- /wp-admin/")) shouldBe true
            (defaults.contains("- /wp-content/")) shouldBe true
            (defaults.contains("- /wp-includes/")) shouldBe true
            (defaults.contains("- /laravel/")) shouldBe true
            (defaults.contains("nginx_base_config_scanner_rate_limit_enabled: false")) shouldBe true
            (defaults.contains("nginx_base_config_scanner_rate_limit_dry_run: true")) shouldBe true
            (defaults.contains("nginx_base_config_scanner_rate_limit_status: 429")) shouldBe true

            (tasks.contains("Validate nginx scanner block settings")) shouldBe true
            (tasks.contains("nginx_base_config_scanner_block_status | int in [404, 444]")) shouldBe true
            (tasks.contains("nginx_base_config_scanner_exact_paths is sequence")) shouldBe true
            (tasks.contains("nginx_base_config_scanner_exact_paths is not string")) shouldBe true
            (tasks.contains("nginx_base_config_scanner_exact_paths | select('string')")) shouldBe true
            (tasks.contains("reject('match', '^/[A-Za-z0-9._/-]+\$')")) shouldBe true
            (tasks.contains("nginx_base_config_scanner_prefix_paths is sequence")) shouldBe true
            (tasks.contains("nginx_base_config_scanner_prefix_paths is not string")) shouldBe true
            (tasks.contains("nginx_base_config_scanner_prefix_paths | select('string')")) shouldBe true
            (tasks.contains("reject('match', '^/[A-Za-z0-9._/-]+/\$')")) shouldBe true

            (infraReadme.contains("Scanner/bot nginx 차단 정책")) shouldBe true
            (infraReadme.contains("기본 응답은 `404`")) shouldBe true
            (infraReadme.contains("`444`는 운영 access log와 smoke 검증 후에만 선택")) shouldBe true
            (infraReadme.contains("rate limit은 1차 rollout에서 강제 적용하지 않는다")) shouldBe true
            (infraReadme.contains("app request completion log를 추가하지 않는다")) shouldBe true
        }


            test("observabilityOwnsSentryFullObservabilityContract") {
            val versionCatalog = read("gradle/libs.versions.toml")
            val rootBuild = read("build.gradle.kts")
            val buildLogicBuild = read("build-logic/build.gradle.kts")
            val apisBuild = read("apis/build.gradle.kts")
            val adminBuild = read("admin/build.gradle.kts")
            val batchBuild = read("batch/build.gradle.kts")
            val observabilityBuild = read("observability/build.gradle.kts")
            val observabilityYaml = read("observability/src/main/resources/application-observability.yml")
            val log4j2 = read("observability/src/main/resources/log4j2-spring.xml")
            val appContainerEnv = read("infra/ansible/roles/app_container_runtime/tasks/env.yml")
            val appRollback = read("infra/ansible/roles/app_rollback/tasks/main.yml")
            val appBluegreen = read("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml")
            val appStopstart = read("infra/ansible/roles/app_stopstart/tasks/run_container.yml")
            val ciPr = read(".github/workflows/ci-pr.yml")
            val deployDev = read(".github/workflows/deploy-dev.yml")
            val deployProd = read(".github/workflows/deploy-prod.yml")
            val observabilityReadme = read("observability/README.md")
            val infraReadme = read("core/infra/README.md")

            (versionCatalog.contains("sentry = \"8.41.0\"")) shouldBe true
            (versionCatalog.contains("sentry-gradle-plugin = \"6.6.0\"")) shouldBe true
            (versionCatalog.contains("io.sentry:sentry-spring-boot-4-starter")) shouldBe true
            (versionCatalog.contains("io.sentry:sentry-async-profiler")) shouldBe true
            (versionCatalog.contains("io.sentry:sentry-log4j2")) shouldBe true
            (rootBuild.contains("includeSourceContext.set(true)")) shouldBe false
            (rootBuild.contains("autoUploadSourceContext.set(")) shouldBe false
            (rootBuild.contains("authToken.set(providers.environmentVariable(\"SENTRY_AUTH_TOKEN\")")) shouldBe false
            (rootBuild.contains("resolutionStrategy.force(\"io.sentry:sentry:\$sentrySdkVersion\")")) shouldBe false
            (buildLogicBuild.contains("sentry-gradle-plugin")) shouldBe true
            (buildLogicBuild.contains("io.sentry.jvm.gradle.gradle.plugin")) shouldBe true
            for (moduleBuild in arrayOf(
                "apis/build.gradle.kts",
                "admin/build.gradle.kts",
                "batch/build.gradle.kts",
                "observability/build.gradle.kts",
                "core/infra/build.gradle.kts",
                "core/domain/build.gradle.kts",
                "gateway/build.gradle.kts"
            )) {
                (read(moduleBuild).contains("id(\"beat.sentry-source-context\")")) shouldBe true
            }
            (observabilityBuild.contains("libs.sentry.spring.boot.starter")) shouldBe true
            (observabilityBuild.contains("libs.sentry.async.profiler")) shouldBe true
            (observabilityBuild.contains("libs.sentry.log4j2")) shouldBe true
            (apisBuild.contains("id(\"beat.prometheus-runtime\")")) shouldBe true
            (batchBuild.contains("id(\"beat.prometheus-runtime\")")) shouldBe true
            (adminBuild.contains("id(\"beat.prometheus-runtime\")")) shouldBe true
            (apisBuild.contains("implementation(libs.micrometer.registry.prometheus)")) shouldBe false
            (observabilityBuild.contains("libs.micrometer.registry.prometheus")) shouldBe false

            (observabilityYaml.contains("dsn: \${SENTRY_DSN:}")) shouldBe true
            (observabilityYaml.contains("sample-rate: 1.0")) shouldBe true
            (observabilityYaml.contains("send-default-pii: true")) shouldBe true
            (observabilityYaml.contains("enabled: true")) shouldBe true
            // Sentry distributed tracing and profiling are intentionally disabled (0.0).
            // Sentry is used for error event capture and Sentry Logs only.
            (observabilityYaml.contains("traces-sample-rate: 0.0")) shouldBe true
            (observabilityYaml.contains("profile-session-sample-rate: 0.0")) shouldBe true
            (observabilityYaml.contains("profile-lifecycle: TRACE")) shouldBe true
            (observabilityYaml.contains("DEV_SENTRY_DSN")) shouldBe false
            (observabilityYaml.contains("PROD_SENTRY_DSN")) shouldBe false
            (observabilityYaml.contains("PROD_SENTRY_TRACES_SAMPLE_RATE")) shouldBe false
            (observabilityYaml.contains("PROD_SENTRY_PROFILE_SESSION_SAMPLE_RATE")) shouldBe false
            (observabilityYaml.contains("enable-tracing")) shouldBe false
            (log4j2.contains("<Sentry name=\"SentryAppender\"")) shouldBe true
            (log4j2.contains("<AppenderRef ref=\"SentryAppender\"/>")) shouldBe true

            (appContainerEnv.contains("'SENTRY_RELEASE': 'beat-server@' ~ (")) shouldBe true
            (appContainerEnv.contains("app_container_runtime_release_ref")) shouldBe true
            (appContainerEnv.contains("default(commit_sha | default(image_tag | default('unknown', true), true), true)")) shouldBe true
            (appRollback.contains("app_rollback_previous_release.commit_sha")) shouldBe true
            (appRollback.contains("app_rollback_previous_release.image_tag")) shouldBe true
            (appBluegreen.contains("app_container_runtime_release_ref: \"{{ app_bluegreen_release_ref | default('', true) }}\"")) shouldBe true
            (appStopstart.contains("app_container_runtime_release_ref: \"{{ app_stopstart_release_ref | default('', true) }}\"")) shouldBe true
            (ciPr.contains("SENTRY_AUTH_TOKEN: \${{ secrets.SENTRY_AUTH_TOKEN }}")) shouldBe true
            (ciPr.contains("SENTRY_RELEASE: beat-server@\${{ github.sha }}")) shouldBe true
            // deploy-dev resolves the deploy ref via `resolve-ref` step, so SENTRY_RELEASE pins to the resolved commit.
            (deployDev.contains("SENTRY_RELEASE: beat-server@\${{ needs.resolve-ref.outputs.commit_sha }}")) shouldBe true
            (deployProd.contains("SENTRY_RELEASE: beat-server@\${{ needs.resolve-release.outputs.commit_sha }}")) shouldBe true
            (appContainerEnv.contains("SENTRY_AUTH_TOKEN")) shouldBe false
            (observabilityReadme.contains("Sentry는 `observability` 모듈이 소유")) shouldBe true
            (observabilityReadme.contains(
                "Sentry integration은 기존 `AccessLogEmitter` 외에 별도 request completion log를 추가하지 않습니다")) shouldBe true
            (infraReadme.contains("SENTRY_DSN=https://public@example.ingest.sentry.io/project-id")) shouldBe true
            (infraReadme.contains("DEV_SENTRY_DSN=")) shouldBe false
            (infraReadme.contains("PROD_SENTRY_DSN=")) shouldBe false
            (infraReadme.contains("SENTRY_AUTH_TOKEN=<Sentry organization token")) shouldBe true
        }


            test("inventoryAndSecurityConfigsOwnEnvironmentSpecificHealthContracts") {
            val localDevSecretScript = read("scripts/generate-local-dev-secret.sh")
            val localProdSecretScript = read("scripts/generate-local-prod-secret.sh")
            val localVarsHelper = read("scripts/lib/local-vars.sh")
            val ansibleConfig = read("infra/ansible/ansible.cfg")
            val sopsConfig = read(".sops.yaml")

            (Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all/main.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all/main.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all.sops.yml"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all.sops.yml"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all.sops.example.yml"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all.sops.example.yml"))) shouldBe false
            (Files.exists(Path.of(".sops.example.yaml"))) shouldBe false
            (Files.exists(Path.of(".sops.yaml"))) shouldBe true
            (Files.exists(Path.of("scripts/lib/local-vars.sh"))) shouldBe true
            (sopsConfig.contains("group_vars/all/.*\\.sops\\.yml")) shouldBe true
            (sopsConfig.contains("age1replacewithdevrecipientkey")) shouldBe false
            (read(".github/workflows/deploy-dev.yml").contains(".sops.yaml")) shouldBe true
            (ansibleConfig.contains("vars_plugins_enabled = host_group_vars,community.sops.sops")) shouldBe true
            (ansibleConfig.contains("vars_stage = inventory")) shouldBe true
            (ansibleConfig.contains("age_ssh_private_keyfile = ~/.ssh/beat-dev")) shouldBe false
            (localDevSecretScript.contains("DEV_ACTUATOR_PORT")) shouldBe true
            (localDevSecretScript.contains("DEV_ACTUATOR_PATH")) shouldBe true
            (localDevSecretScript.contains("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml")) shouldBe true
            (localDevSecretScript.contains("all.local.sops.yml")) shouldBe false
            (localDevSecretScript.contains("all.sops.example.yml")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_port:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_path:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_upstream_port:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_public_path:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("nginx_server_name:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("nginx_server_name:")) shouldBe true
            (read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("letsencrypt_cert_name:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("letsencrypt_cert_name:")) shouldBe true
            (read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_allow_cidrs:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("actuator_allow_cidrs:")) shouldBe true
            (read("infra/ansible/inventories/dev/hosts.yml").contains("ansible_host:")) shouldBe false
            (read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("ansible_host:")) shouldBe true
            (localDevSecretScript.contains("sops -d --extract '[\"actuator_port\"]'")) shouldBe true
            (localProdSecretScript.contains("PROD_ACTUATOR_PORT")) shouldBe true
            (localProdSecretScript.contains("PROD_ACTUATOR_PATH")) shouldBe true
            (localProdSecretScript.contains("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml")) shouldBe true
            (localProdSecretScript.contains("all.local.sops.yml")) shouldBe false
            (localProdSecretScript.contains("all.sops.example.yml")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_port:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_path:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_upstream_port:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_public_path:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("nginx_server_name:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("nginx_server_name:")) shouldBe true
            (read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("letsencrypt_cert_name:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("letsencrypt_cert_name:")) shouldBe true
            (read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_allow_cidrs:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("actuator_allow_cidrs:")) shouldBe true
            (read("infra/ansible/inventories/prod/hosts.yml").contains("ansible_host:")) shouldBe false
            (read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("ansible_host:")) shouldBe true
            (localProdSecretScript.contains("sops -d --extract '[\"actuator_port\"]'")) shouldBe true
            (localVarsHelper.contains("require_sops_identity")) shouldBe true
            (localVarsHelper.contains("read_yaml_value")) shouldBe false
            (localVarsHelper.contains("\$HOME/.ssh/beat-dev")) shouldBe false
            (ansibleConfig.contains("host_key_checking = False")) shouldBe false
            (ansibleConfig.contains("StrictHostKeyChecking=no")) shouldBe false
        }

    }
}

internal fun read(path: String): String = Files.readString(Path.of(path))

internal fun sectionAfter(content: String, marker: String): String {
    val start = content.indexOf(marker)
    (start >= 0) shouldBe true
    val nextDocument = content.indexOf("\n---", start + marker.length)
    return if (nextDocument < 0) content.substring(start) else content.substring(start, nextDocument)
}

internal fun sectionBetween(content: String, startMarker: String, endMarker: String): String {
    val start = content.indexOf(startMarker)
    (start >= 0) shouldBe true
    val end = content.indexOf(endMarker, start + startMarker.length)
    (end >= 0) shouldBe true
    return content.substring(start, end)
}

internal fun countOccurrences(content: String, needle: String): Int {
    var count = 0
    var index = 0
    while (true) {
        index = content.indexOf(needle, index)
        if (index < 0) break
        count++
        index += needle.length
    }
    return count
}

internal fun assertBefore(content: String, first: String, second: String) {
    val firstIndex = content.indexOf(first)
    val secondIndex = content.indexOf(second)
    (firstIndex >= 0) shouldBe true
    (secondIndex >= 0) shouldBe true
    (firstIndex < secondIndex) shouldBe true
}
