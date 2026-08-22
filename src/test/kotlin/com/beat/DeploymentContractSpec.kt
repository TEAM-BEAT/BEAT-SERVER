package com.beat

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

class DeploymentContractSpec : FunSpec() {
    init {
            test("legacyDeploymentEntryPointsStayRetired") {
            (Files.exists(Path.of(".github/workflows/dev-CI.yml"))) shouldBe false
            (Files.exists(Path.of(".github/workflows/prod-CI.yml"))) shouldBe false
            (Files.exists(Path.of("Dockerfile"))) shouldBe false
            (Files.exists(Path.of("Dockerfile-dev"))) shouldBe false
            (Files.exists(Path.of("Jenkinsfile"))) shouldBe false
            (Files.exists(Path.of(".github/workflows/v2-web-deploy-dev.yml"))) shouldBe false
            (Files.exists(Path.of(".github/workflows/v2-web-deploy-prod.yml"))) shouldBe false
        }


            test("devAndReusableDeploymentContractsUseSharedToolingAndInventoryOwnedSshMetadata") {
            val ciPr = read(".github/workflows/ci-pr.yml")
            val ansibleLintWorkflow = read(".github/workflows/ansible-lint.yml")
            val ansibleExecWorkflow = read(".github/workflows/_ansible-exec.yml")
            val trivyImageConfig = read(".trivy-image.yaml")
            val deployDev = read(".github/workflows/deploy-dev.yml")
            val deployProd = read(".github/workflows/deploy-prod.yml")
            val rollbackProd = read(".github/workflows/rollback-prod.yml")
            val setupAnsibleTooling = read(".github/actions/setup-ansible-tooling/action.yml")
            val setupSshClient = read(".github/actions/setup-ssh-client/action.yml")
            val resolveAnsibleConnection = read(".github/actions/resolve-ansible-connection/action.yml")

            (Files.exists(Path.of(".github/workflows/deploy-dev.yml"))) shouldBe true
            (Files.exists(Path.of(".github/workflows/deploy-prod.yml"))) shouldBe true
            (Files.exists(Path.of(".github/workflows/rollback-prod.yml"))) shouldBe true
            (Files.exists(Path.of(".github/workflows/_ansible-exec.yml"))) shouldBe true
            (Files.exists(Path.of(".github/workflows/ansible-lint.yml"))) shouldBe true
            (Files.exists(Path.of(".trivy-image.yaml"))) shouldBe true
            (ciPr.contains("./gradlew check verifyModuleBootJars --parallel --build-cache")) shouldBe true
            (ciPr.contains("verifyV2WebBaseline")) shouldBe false
            (deployDev.contains("./gradlew check verifyModuleBootJars --parallel --build-cache")) shouldBe true
            (deployDev.contains("verifyV2WebBaseline")) shouldBe false
            (ciPr.contains("verifyModuleBootJars")) shouldBe true
            (ciPr.contains("matrix:")) shouldBe true
            (ciPr.contains("- apis")) shouldBe true
            (ciPr.contains("- admin")) shouldBe true
            (ciPr.contains("- batch")) shouldBe true
            (ciPr.contains("MODULE=\${{ matrix.module }}")) shouldBe true
            (ciPr.contains("aquasecurity/trivy-action@ed142fd0673e97e23eac54620cfb913e5ce36c25")) shouldBe true
            (ciPr.contains("scan-type: image")) shouldBe true
            (ciPr.contains("trivy-config: .trivy-image.yaml")) shouldBe true
            (trivyImageConfig.contains("ignore-unfixed: true")) shouldBe true
            (trivyImageConfig.contains("vuln-type: os,library")) shouldBe true
            (trivyImageConfig.contains("severity: CRITICAL,HIGH")) shouldBe true
            (deployDev.contains("dorny/paths-filter")) shouldBe true
            (deployDev.contains("fromJSON(")) shouldBe true
            (deployDev.contains("uses: ./.github/workflows/_ansible-exec.yml")) shouldBe true
            (deployProd.contains("uses: ./.github/workflows/_ansible-exec.yml")) shouldBe true
            (deployProd.contains("secret-preflight:")) shouldBe true
            (deployProd.contains("Resolve prod SSH connection metadata")) shouldBe true
            (deployProd.contains("Verify prod encrypted inventory, resolver, and lint")) shouldBe true
            (deployProd.contains("Prod secret-aware preflight verified resolver for module=\${MODULE}")) shouldBe true
            (deployProd.contains("ansible-lint playbooks/*.yml roles")) shouldBe true
            (deployProd.contains("git merge-base --is-ancestor \"\$COMMIT_SHA\" refs/remotes/origin/main")) shouldBe true
            (rollbackProd.contains("uses: ./.github/workflows/_ansible-exec.yml")) shouldBe true
            (deployDev.contains("environment_name: dev")) shouldBe true
            (deployProd.contains("environment_name: prod")) shouldBe true
            (rollbackProd.contains("environment_name: prod")) shouldBe true
            (deployDev.contains("secrets: inherit")) shouldBe false
            (deployProd.contains("secrets: inherit")) shouldBe false
            (rollbackProd.contains("secrets: inherit")) shouldBe false
            (deployDev.contains("ssh_host: \${{")) shouldBe false
            (deployProd.contains("ssh_host: \${{")) shouldBe false
            (rollbackProd.contains("ssh_host: \${{")) shouldBe false
            (deployDev.contains("ssh_host_fingerprint: \${{")) shouldBe false
            (deployProd.contains("ssh_host_fingerprint: \${{")) shouldBe false
            (rollbackProd.contains("ssh_host_fingerprint: \${{")) shouldBe false
            (deployDev.contains("ssh_private_key: \${{ secrets.DEV_SSH_PRIVATE_KEY }}")) shouldBe true
            (deployProd.contains("ssh_private_key: \${{ secrets.PROD_SSH_PRIVATE_KEY }}")) shouldBe true
            (rollbackProd.contains("ssh_private_key: \${{ secrets.PROD_SSH_PRIVATE_KEY }}")) shouldBe true
            (ansibleExecWorkflow.contains("workflow_call:")) shouldBe true
            (ansibleExecWorkflow.contains("checkout_ref:")) shouldBe true
            (ansibleExecWorkflow.contains("environment: \${{ inputs.environment_name }}")) shouldBe true
            (ansibleExecWorkflow.contains("Setup Ansible tooling")) shouldBe true
            (ansibleExecWorkflow.contains("resolve-ansible-connection")) shouldBe true
            (ansibleExecWorkflow.contains("setup-ssh-client")) shouldBe true
            (ansibleExecWorkflow.contains("cmd=(ansible-playbook")) shouldBe true
            (ansibleExecWorkflow.contains("EXTRA_VARS_PATH=\"\$(mktemp /tmp/beat-extra-vars.")) shouldBe true
            (ansibleExecWorkflow.contains("echo \"EXTRA_VARS_PATH=\$EXTRA_VARS_PATH\" >> \"\$GITHUB_ENV\"")) shouldBe true
            (ansibleExecWorkflow.contains("cmd+=(--extra-vars \"@\$EXTRA_VARS_PATH\")")) shouldBe true
            (ansibleExecWorkflow.contains("Cleanup temporary credentials")) shouldBe true
            (ansibleExecWorkflow.contains("Notify Slack (success)")) shouldBe true
            (ansibleExecWorkflow.contains("ssh_host:")) shouldBe false
            (ansibleExecWorkflow.contains("ssh_port:")) shouldBe false
            (ansibleExecWorkflow.contains("ssh_host_fingerprint:")) shouldBe false
            (ansibleExecWorkflow.contains("ssh_private_key:")) shouldBe true
            (ansibleExecWorkflow.contains("sops_age_key:")) shouldBe true
            (ansibleExecWorkflow.contains("slack_webhook_url:")) shouldBe true
            (ansibleExecWorkflow.contains("continue-on-error: true")) shouldBe false
            (ansibleExecWorkflow.contains("LEGACY_HOST:")) shouldBe false
            (ansibleExecWorkflow.contains("LEGACY_PORT:")) shouldBe false
            (ansibleExecWorkflow.contains("SSH_CONNECTION_SOURCE")) shouldBe false
            (ansibleExecWorkflow.contains("HAS_SLACK_WEBHOOK")) shouldBe true
            (ansibleExecWorkflow.contains("ssh-private-key: \${{ secrets.ssh_private_key }}")) shouldBe true
            (ansibleExecWorkflow.contains("SOPS_AGE_KEY: \${{ secrets.sops_age_key }}")) shouldBe true
            (ansibleExecWorkflow.contains("ANSIBLE_SOPS_AGE_SSH_PRIVATE_KEYFILE")) shouldBe false
            (ansibleExecWorkflow.contains("SOPS_AGE_SSH_PRIVATE_KEY_FILE=\"\$HOME/.ssh/deploy_key\"")) shouldBe false
            (ansibleExecWorkflow.contains("SLACK_WEBHOOK_URL: \${{ secrets.slack_webhook_url }}")) shouldBe true
            (ansibleExecWorkflow.contains("python3 - \"\$EXTRA_VARS_PATH\" <<'PY'")) shouldBe true
            (ansibleExecWorkflow.contains("inventory_sops_path:")) shouldBe false
            (ansibleExecWorkflow.contains("INVENTORY_SOPS_PATH")) shouldBe false
            (ansibleExecWorkflow.contains("inventory_label:")) shouldBe false
            (ansibleExecWorkflow.contains("INVENTORY_LABEL")) shouldBe false
            (ansibleExecWorkflow.contains("sops -d \"\$INVENTORY_SOPS_PATH\"")) shouldBe false
            (ansibleExecWorkflow.contains(
                "inputs.environment_name == 'dev' && secrets.DEV_SSH_HOST || secrets.PROD_SSH_HOST")) shouldBe false
            (ansibleExecWorkflow.contains("PROD_DOCKER_LOGIN_USERNAME")) shouldBe false
            (setupAnsibleTooling.contains("sigstore/cosign-installer@6f9f17788090df1f26f669e9d70d6ae9567deba6")) shouldBe true
            (setupAnsibleTooling.contains("Install verified age")) shouldBe true
            (setupAnsibleTooling.contains("cosign verify-blob")) shouldBe true
            (setupAnsibleTooling.contains("sha256sum -c")) shouldBe true
            (setupAnsibleTooling.contains("ansible_core-2.17.14-py3-none-any.whl")) shouldBe true
            (setupSshClient.contains("ssh-host-fingerprint")) shouldBe true
            (setupSshClient.contains("ssh-keyscan -T 10")) shouldBe true
            (setupSshClient.contains("ssh-keygen -lf - -E sha256")) shouldBe true
            (setupSshClient.contains("Host fingerprint verification failed")) shouldBe true
            (resolveAnsibleConnection.contains("Resolve SSH connection metadata")) shouldBe true
            (resolveAnsibleConnection.contains("Setup Ansible tooling")) shouldBe false
            (ansibleLintWorkflow.contains("ansible-lint")) shouldBe true
            (ansibleLintWorkflow.contains("working-directory: infra/ansible")) shouldBe true
            (ansibleLintWorkflow.contains("ansible-lint playbooks/*.yml roles")) shouldBe true
            (ansibleLintWorkflow.contains(".github/workflows/_ansible-exec.yml")) shouldBe true
            (deployDev.contains(".sops.yaml")) shouldBe true
            (deployDev.contains(".sops.example.yaml")) shouldBe false
            }


            test("prodReleaseDeploymentUsesSharedImmutableVersionAndModuleMatrix") {
            val deployProd = read(".github/workflows/deploy-prod.yml")
            val secretAwareVerify = read(".github/workflows/ansible-secret-aware-verify.yml")

            (deployProd.contains("release:")) shouldBe true
            (deployProd.contains("- published")) shouldBe true
            (deployProd.contains("github.event.release.tag_name")) shouldBe true
            (deployProd.contains("^v[0-9]+\\.[0-9]+\\.[0-9]+\$")) shouldBe true
            (deployProd.contains("Invalid release tag: \$RELEASE_TAG (expected vX.Y.Z)")) shouldBe true
            (deployProd.contains("module_matrix")) shouldBe true
            (deployProd.contains("matrix.module")) shouldBe true
            (deployProd.contains("commit_sha")) shouldBe true
            (deployProd.contains("ref: \${{ needs.resolve-release.outputs.commit_sha }}")) shouldBe true
            (deployProd.contains("checkout_ref: \${{ needs.resolve-release.outputs.commit_sha }}")) shouldBe true
            (deployProd.contains("- secret-preflight")) shouldBe true
            (deployProd.contains("Resolve prod SSH connection metadata")) shouldBe true
            (deployProd.contains("Prod secret-aware preflight verified resolver for module=\${MODULE}")) shouldBe true
            (deployProd.contains("ansible-lint playbooks/*.yml roles")) shouldBe true
            (deployProd.contains("git merge-base --is-ancestor \"\$COMMIT_SHA\" refs/remotes/origin/main")) shouldBe true
            (deployProd.contains("workflow_dispatch:")) shouldBe false
            (deployProd.contains("github.event.inputs.version")) shouldBe false
            (deployProd.contains("github.event.inputs.module")) shouldBe false
            (deployProd.contains("checkout_ref=refs/tags/")) shouldBe false
            val prodModuleMatrix =
                "module_matrix={\"include\":[{\"module\":\"admin\"},{\"module\":\"apis\"},{\"module\":\"batch\"}]}"
            (deployProd.contains(prodModuleMatrix)) shouldBe true

            (secretAwareVerify.contains("module: \${{ matrix.module }}")) shouldBe true
            (secretAwareVerify.contains("Verified resolver for module=\${MODULE}")) shouldBe true
            (secretAwareVerify.contains("- admin")) shouldBe true
            (secretAwareVerify.contains("- batch")) shouldBe true
        }


            test("foundationMarkerContractProtectsDeployAndRollback") {
            val ansibleExecWorkflow = read(".github/workflows/_ansible-exec.yml")
            val deployDev = read(".github/workflows/deploy-dev.yml")
            val deployProd = read(".github/workflows/deploy-prod.yml")
            val foundationPlaybook = read("infra/ansible/playbooks/foundation.yml")
            val deployPlaybook = read("infra/ansible/playbooks/deploy.yml")
            val rollbackPlaybook = read("infra/ansible/playbooks/rollback.yml")
            val nginxFragmentsPreflight = read("infra/ansible/playbooks/tasks/validate_nginx_fragments.yml")
            val infraReadme = read("core/infra/README.md")

            (ansibleExecWorkflow.contains("connection_module:")) shouldBe true
            (ansibleExecWorkflow.contains(
                "module: \${{ inputs.connection_module != '' && inputs.connection_module || inputs.module }}")) shouldBe true

            (foundationPlaybook.contains("foundation_marker_path: \"{{ deployment_dir }}/.foundation-applied\"")) shouldBe true
            (foundationPlaybook.contains("tasks/validate_nginx_fragments.yml")) shouldBe true
            (foundationPlaybook.contains("name: Assert required foundation marker inputs")) shouldBe true
            (foundationPlaybook.contains("deploy_environment is defined")) shouldBe true
            (foundationPlaybook.contains("post_tasks:")) shouldBe true
            (foundationPlaybook.contains("name: Mark foundation as applied")) shouldBe true
            (foundationPlaybook.contains("applied_at: {{ now(utc=true, fmt='%Y-%m-%dT%H:%M:%SZ') }}")) shouldBe true
            (foundationPlaybook.contains("commit_sha: {{ commit_sha | default('unknown') }}")) shouldBe true
            (foundationPlaybook.contains("deploy_environment: {{ deploy_environment }}")) shouldBe true
            (foundationPlaybook.contains("foundation_mysql_enabled: {{ foundation_mysql_enabled | default(true) }}")) shouldBe true
            (foundationPlaybook.contains("foundation_redis_enabled: {{ foundation_redis_enabled | default(true) }}")) shouldBe true
            (foundationPlaybook.contains("foundation_manage_nginx: {{ foundation_manage_nginx | default(false) }}")) shouldBe true
            assertBefore(foundationPlaybook, "role: foundation_stack", "name: Mark foundation as applied")
            assertBefore(foundationPlaybook, "role: nginx_base_config", "name: Mark foundation as applied")

            (deployPlaybook.contains("foundation_marker_path: \"{{ deployment_dir }}/.foundation-applied\"")) shouldBe true
            (deployPlaybook.contains("tasks/validate_nginx_fragments.yml")) shouldBe true
            (deployPlaybook.contains("name: Stat foundation marker")) shouldBe true
            (deployPlaybook.contains("register: deploy_foundation_marker_stat")) shouldBe true
            (deployPlaybook.contains("name: Read foundation marker for diagnostics")) shouldBe true
            (deployPlaybook.contains("register: deploy_foundation_marker_raw")) shouldBe true
            (deployPlaybook.contains("name: Abort deploy when foundation is not applied")) shouldBe true
            (deployPlaybook.contains("Foundation has not been applied on host {{ inventory_hostname }}.")) shouldBe true
            (deployPlaybook.contains("inventories/<env>/hosts.yml")) shouldBe true
            (deployPlaybook.contains("default(['inventories/dev/hosts.yml'])")) shouldBe false
            (deployPlaybook.contains("Or trigger the foundation step on GitHub Actions before retrying deploy.")) shouldBe true
            (deployPlaybook.contains("name: Report foundation marker contents")) shouldBe true
            assertBefore(deployPlaybook, "name: Stat foundation marker", "role: app_secret")

            (rollbackPlaybook.contains("foundation_marker_path: \"{{ deployment_dir }}/.foundation-applied\"")) shouldBe true
            (rollbackPlaybook.contains("tasks/validate_nginx_fragments.yml")) shouldBe true
            (rollbackPlaybook.contains("name: Stat foundation marker")) shouldBe true
            (rollbackPlaybook.contains("register: rollback_foundation_marker_stat")) shouldBe true
            (rollbackPlaybook.contains("name: Read foundation marker for diagnostics")) shouldBe true
            (rollbackPlaybook.contains("register: rollback_foundation_marker_raw")) shouldBe true
            (rollbackPlaybook.contains("name: Abort rollback when foundation is not applied")) shouldBe true
            (rollbackPlaybook.contains("inventories/<env>/hosts.yml")) shouldBe true
            (rollbackPlaybook.contains("default(['inventories/dev/hosts.yml'])")) shouldBe false
            (rollbackPlaybook.contains("Or trigger the foundation step on GitHub Actions before retrying rollback.")) shouldBe true
            (rollbackPlaybook.contains("name: Report foundation marker contents")) shouldBe true
            assertBefore(rollbackPlaybook, "name: Stat foundation marker", "name: Roll back runtime to previous release")

            assertBefore(deployDev, "\n  foundation:", "\n  deploy:")
            val devFoundationNeeds =
                "  foundation:\n" +
                    "    needs:\n" +
                    "      - detect-changes\n" +
                    "      - resolve-ref\n" +
                    "      - verify\n" +
                    "      - build-image"
            val devDeployNeedsFoundation =
                "      - build-image\n" +
                    "      - foundation\n" +
                    "    if: needs.detect-changes.outputs.has_modules == 'true'"
            (deployDev.contains(devFoundationNeeds)) shouldBe true
            (deployDev.contains("module: foundation")) shouldBe true
            (deployDev.contains("connection_module: \${{ vars.DEV_FOUNDATION_CONNECTION_MODULE || 'apis' }}")) shouldBe true
            (deployDev.contains("playbook: playbooks/foundation.yml")) shouldBe true
            // commit_sha / checkout_ref now pin to the resolved deploy ref (supports manual deploy_ref input).
            (deployDev.contains("commit_sha: \${{ needs.resolve-ref.outputs.commit_sha }}")) shouldBe true
            (deployDev.contains("checkout_ref: \${{ needs.resolve-ref.outputs.commit_sha }}")) shouldBe true
            (deployDev.contains(devDeployNeedsFoundation)) shouldBe true
            // dev-runtime concurrency group serializes deploys to the single dev runtime cluster.
            (deployDev.contains("group: dev-runtime")) shouldBe true

            assertBefore(deployProd, "\n  foundation:", "\n  deploy:")
            val prodFoundationNeeds =
                "  foundation:\n" +
                    "    needs:\n" +
                    "      - resolve-release\n" +
                    "      - verify\n" +
                    "      - secret-preflight\n" +
                    "      - build-image"
            val prodDeployNeedsFoundation =
                "      - secret-preflight\n" +
                    "      - build-image\n" +
                    "      - foundation\n" +
                    "    concurrency:\n" +
                    "      group: prod-runtime"
            (deployProd.contains(prodFoundationNeeds)) shouldBe true
            (deployProd.contains("module: foundation")) shouldBe true
            (deployProd.contains("connection_module: \${{ vars.PROD_FOUNDATION_CONNECTION_MODULE || 'apis' }}")) shouldBe true
            (deployProd.contains("playbook: playbooks/foundation.yml")) shouldBe true
            (deployProd.contains("commit_sha: \${{ needs.resolve-release.outputs.commit_sha }}")) shouldBe true
            (deployProd.contains("checkout_ref: \${{ needs.resolve-release.outputs.commit_sha }}")) shouldBe true
            (deployProd.contains(prodDeployNeedsFoundation)) shouldBe true
            (deployProd.contains("group: prod-runtime")) shouldBe true
            (infraReadme.contains(
                "`deploy-prod.yml`의 `resolve-release` → `verify` + `secret-preflight` → `build-image` → `foundation` → `deploy` 순서를 확인한다.")) shouldBe true

            (infraReadme.contains("Foundation marker contract")) shouldBe true
            (infraReadme.contains("{{ deployment_dir }}/.foundation-applied")) shouldBe true
            (infraReadme.contains("applied_at`, `commit_sha`, `deploy_environment`")) shouldBe true
            (infraReadme.contains("DEV_FOUNDATION_CONNECTION_MODULE")) shouldBe true
            (infraReadme.contains("PROD_FOUNDATION_CONNECTION_MODULE")) shouldBe true
            (nginxFragmentsPreflight.contains("nginx_fragments is mapping")) shouldBe true
            (nginxFragmentsPreflight.contains("nginx_fragments mapping has invalid or duplicate entries")) shouldBe true
            (nginxFragmentsPreflight.contains("nginx_fragment_files | unique | list | length")) shouldBe true
            (nginxFragmentsPreflight.contains("modules is mapping")) shouldBe true
            (nginxFragmentsPreflight.contains("((modules | default({})).apis | default({})).backend_upstream_name")) shouldBe true
            (nginxFragmentsPreflight.contains(
                "(((modules | default({})).admin | default({})).nginx_route | default({})).upstream_name")) shouldBe true
        }


            test("deploymentInfraUsesRepoOwnedHelpersAndConfiguredModuleContracts") {
            val dockerfileModule = read("Dockerfile.module")
            val dockerignore = read(".dockerignore")
            val nginxUpdateScript = read("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py")
            val foundationPlaybook = read("infra/ansible/playbooks/foundation.yml")
            val foundationStackTasks = read("infra/ansible/roles/foundation_stack/tasks/main.yml")
            val foundationComposeTemplate = read("infra/ansible/roles/foundation_stack/templates/foundation.compose.yml.j2")
            val defaultConfTemplate = read("infra/ansible/roles/nginx_base_config/templates/default.conf.j2")
            val deployPlaybook = read("infra/ansible/playbooks/deploy.yml")
            val rollbackPlaybook = read("infra/ansible/playbooks/rollback.yml")
            val appSecretRole = read("infra/ansible/roles/app_secret/tasks/main.yml")
            val appScriptsRole = read("infra/ansible/roles/app_scripts/tasks/main.yml")
            val appBluegreenRunSwitch = read("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml")
            val appStopStartRole = read("infra/ansible/roles/app_stopstart/tasks/main.yml")
            val appStopStartRunContainer = read("infra/ansible/roles/app_stopstart/tasks/run_container.yml")
            val appContainerRuntimeEnv = read("infra/ansible/roles/app_container_runtime/tasks/env.yml")
            val appHealthcheckRole = read("infra/ansible/roles/app_healthcheck/tasks/main.yml")
            val appHealthcheckProbe = read("infra/ansible/roles/app_healthcheck/tasks/probe.yml")
            val appCleanupRole = read("infra/ansible/roles/app_cleanup/tasks/main.yml")
            val appRollbackRole = read("infra/ansible/roles/app_rollback/tasks/main.yml")
            val infraReadme = read("core/infra/README.md")
            val nginxBaseConfig = read("infra/ansible/roles/nginx_base_config/tasks/main.yml")
            val adminNginxRoute = read("infra/ansible/roles/app_stopstart/tasks/admin_nginx_route.yml")
            val deployDev = read(".github/workflows/deploy-dev.yml")
            val deployProd = read(".github/workflows/deploy-prod.yml")
            val rollbackProd = read(".github/workflows/rollback-prod.yml")
            val devInventory = read("infra/ansible/inventories/dev/group_vars/all/main.yml")
            val prodInventory = read("infra/ansible/inventories/prod/group_vars/all/main.yml")

            (Files.exists(Path.of("infra/ansible/playbooks/deploy.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/playbooks/rollback.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/playbooks/foundation.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/files/deploy-blue-green.sh"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/files/deploy-stop-start.sh"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/files/deploy-common.sh"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/roles/app_dev_switch"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/roles/app_prod_switch"))) shouldBe false
            (Files.exists(Path.of("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/roles/foundation_stack/templates/foundation.compose.yml.j2"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/roles/nginx_base_config/templates/default.conf.j2"))) shouldBe true
            (Files.exists(Path.of("infra/ansible/roles/nginx_config_helper/tasks/migrate_legacy_upstreams.yml"))) shouldBe false
            (Files.exists(Path.of("scripts/generate-local-dev-secret.sh"))) shouldBe true
            (Files.exists(Path.of("scripts/generate-local-prod-secret.sh"))) shouldBe true
            (Files.exists(Path.of(".dockerignore"))) shouldBe true
            (dockerfileModule.contains("ARG MODULE")) shouldBe true
            // JAR is built outside the container (ARM native build) and COPY'd in — no in-container build stage.
            (dockerfileModule.contains("COPY --chown=beat:beat app.jar /app/app.jar")) shouldBe true
            (dockerfileModule.contains(
                "ENTRYPOINT [\"java\", \"-Duser.timezone=Asia/Seoul\", \"-jar\", \"/app/app.jar\"]")) shouldBe true
            (dockerfileModule.contains("COPY src ./src")) shouldBe false
            (dockerfileModule.contains("COPY --from=build /app/secret")) shouldBe false
            (dockerfileModule.contains("SERVER_PORT")) shouldBe false
            (dockerfileModule.contains("EXPOSE")) shouldBe false
            (dockerignore.contains(".git")) shouldBe true
            (dockerignore.contains("**/build")) shouldBe true
            (dockerignore.contains(".omx")) shouldBe true
            (dockerignore.contains("src/")) shouldBe true
            (appBluegreenRunSwitch.contains("community.docker.docker_container")) shouldBe true
            (appBluegreenRunSwitch.contains("name: nginx_fragment_transaction")) shouldBe true
            (appBluegreenRunSwitch.contains("nginx_fragment_transaction_files:")) shouldBe true
            (appBluegreenRunSwitch.contains("nginx_fragment_transaction_operations:")) shouldBe true
            (appBluegreenRunSwitch.contains("app_bluegreen_backend_upstream_source_path")) shouldBe true
            (appBluegreenRunSwitch.contains("app_bluegreen_backend_upstream_target_path")) shouldBe true
            (appBluegreenRunSwitch.contains("app_bluegreen_actuator_upstream_source_path")) shouldBe true
            (appBluegreenRunSwitch.contains("app_bluegreen_actuator_upstream_target_path")) shouldBe true
            (appBluegreenRunSwitch.contains("Backup current nginx source config")) shouldBe false
            (appBluegreenRunSwitch.contains("Backup current nginx target config")) shouldBe false
            (appBluegreenRunSwitch.contains("Backup current managed upstream fragment")) shouldBe false
            (appBluegreenRunSwitch.contains("current-slot")) shouldBe true
            (appBluegreenRunSwitch.contains("upsert-upstream")) shouldBe true
            (appBluegreenRunSwitch.contains("public_smoke_url")) shouldBe true
            (appBluegreenRunSwitch.contains("app_container_env")) shouldBe true
            (appBluegreenRunSwitch.contains("name: app_container_runtime")) shouldBe true
            (appBluegreenRunSwitch.contains(
                "healthcheck_target_container: \"{{ app_bluegreen_target_container }}\"")) shouldBe true
            (appContainerRuntimeEnv.contains("| combine({")) shouldBe true
            (appContainerRuntimeEnv.contains("'SPRING_PROFILES_ACTIVE': app_container_runtime_module_cfg.spring_profile")) shouldBe true
            (appBluegreenRunSwitch.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\"")) shouldBe false
            (appBluegreenRunSwitch.contains("SPRING_PROFILES_ACTIVE")) shouldBe false
            (appStopStartRole.contains("run_container.yml")) shouldBe true
            (appStopStartRunContainer.contains("community.docker.docker_container")) shouldBe true
            (appContainerRuntimeEnv.contains("BEAT_SCHEDULER_OWNER")) shouldBe true
            (appStopStartRunContainer.contains("app_container_env")) shouldBe true
            (appStopStartRunContainer.contains("name: app_container_runtime")) shouldBe true
            (appStopStartRunContainer.contains(
                "healthcheck_target_container: \"{{ app_stopstart_module_cfg.container_name }}\"")) shouldBe true
            (appStopStartRunContainer.contains("| default(module)")) shouldBe false
            (appContainerRuntimeEnv.contains("| combine({")) shouldBe true
            (appContainerRuntimeEnv.contains("| string | lower")) shouldBe true
            (appStopStartRunContainer.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\"")) shouldBe false
            (appStopStartRunContainer.contains("SPRING_PROFILES_ACTIVE")) shouldBe false
            (appStopStartRunContainer.contains("\n    ports:")) shouldBe false
            (appStopStartRunContainer.contains("0.0.0.0:400")) shouldBe false
            (appBluegreenRunSwitch.contains("\n        ports:")) shouldBe false
            (appBluegreenRunSwitch.contains("0.0.0.0:400")) shouldBe false
            (foundationPlaybook.contains("role: foundation_stack")) shouldBe true
            (foundationPlaybook.contains("role: nginx_base_config")) shouldBe true
            (foundationStackTasks.contains("project_src: \"{{ deployment_dir }}\"")) shouldBe true
            (foundationStackTasks.contains("- docker-compose.yml")) shouldBe true
            (foundationStackTasks.contains("Ensure nginx bind mount and candidate directories exist")) shouldBe true
            (foundationStackTasks.contains("Migrate legacy nginx named volume config to bind mount")) shouldBe true
            (foundationStackTasks.contains("Migrate legacy nginx named volume fragments to bind mount")) shouldBe true
            (foundationStackTasks.contains("- -anv")) shouldBe true
            (foundationStackTasks.contains("foundation_stack_legacy_config_migration_result.stdout")) shouldBe true
            (foundationStackTasks.contains("Inspect legacy nginx named volume metadata")) shouldBe true
            (foundationStackTasks.contains("community.docker.docker_volume_info")) shouldBe true
            (foundationStackTasks.contains("foundation_stack_legacy_nginx_volume_info.exists")) shouldBe true
            (foundationStackTasks.contains("when: not foundation_stack_bind_migration_marker_stat.stat.exists")) shouldBe true
            (foundationStackTasks.contains("- docker\n      - volume\n      - inspect")) shouldBe false
            (foundationStackTasks.contains(".bind-mount-migrated-from-{{ foundation_stack_legacy_nginx_volume_name")) shouldBe true
            (foundationStackTasks.contains("Remove stale nginx helper lock files from deployment-owned nginx tree")) shouldBe true
            (foundationStackTasks.contains("/var/lib/docker/volumes")) shouldBe false
            (foundationStackTasks.contains("foundation_stack_compose_definition")) shouldBe false
            (foundationStackTasks.contains("definition: \"{{ foundation_stack_compose_definition }}\"")) shouldBe false
            (foundationComposeTemplate.contains("services:")) shouldBe true
            (foundationComposeTemplate.contains("container_name: \"{{ nginx_container_name }}\"")) shouldBe true
            (foundationComposeTemplate.contains("- \"80:80\"")) shouldBe true
            (foundationComposeTemplate.contains("- \"443:443\"")) shouldBe true
            (foundationComposeTemplate.contains("{{ deployment_dir }}/nginx/conf.d:/etc/nginx/conf.d")) shouldBe true
            (foundationComposeTemplate.contains("{{ deployment_dir }}/nginx/generated:/etc/nginx/generated")) shouldBe true
            (foundationComposeTemplate.contains(":/etc/nginx\"")) shouldBe false
            (foundationComposeTemplate.contains("nginx-config-volume")) shouldBe false
            (foundationComposeTemplate.contains("foundation_mysql_enabled")) shouldBe true
            (foundationComposeTemplate.contains("- \"127.0.0.1:3306:3306\"")) shouldBe true
            (foundationComposeTemplate.contains("foundation_redis_enabled")) shouldBe true
            (defaultConfTemplate.contains("upstream {{ backend_upstream_name")) shouldBe false
            (defaultConfTemplate.contains("upstream {{ actuator_upstream_name")) shouldBe false
            (defaultConfTemplate.contains("location {{ actuator_path }}/")) shouldBe true
            (defaultConfTemplate.contains("location /admin/")) shouldBe false
            (defaultConfTemplate.contains("BEAT MANAGED GENERATED UPSTREAM INCLUDES")) shouldBe true
            (defaultConfTemplate.contains("BEAT MANAGED GENERATED ROUTE INCLUDES")) shouldBe true
            (defaultConfTemplate.contains("escape=json")) shouldBe true
            (defaultConfTemplate.contains("\"trace_id\":\"\$effective_trace_id\"")) shouldBe true
            (defaultConfTemplate.contains("\"request_id\":\"\$request_id\"")) shouldBe true
            (defaultConfTemplate.contains("map \$http_traceparent \$trace_parent")) shouldBe true
            (defaultConfTemplate.contains("~*^00-")) shouldBe false
            (defaultConfTemplate.contains("default \$generated_trace_parent")) shouldBe true
            (defaultConfTemplate.contains("\"client_ip\":\"\$remote_addr\"")) shouldBe true
            (defaultConfTemplate.contains("\"request\":\"\$request\"")) shouldBe true
            (defaultConfTemplate.contains("\"status\":\"\$status\"")) shouldBe true
            (defaultConfTemplate.contains("\"bytes\":\$body_bytes_sent")) shouldBe true
            (defaultConfTemplate.contains("\"referer\":\"\$http_referer\"")) shouldBe true
            (defaultConfTemplate.contains("\"user_agent\":\"\$http_user_agent\"")) shouldBe true
            (defaultConfTemplate.contains("\"x_forwarded_for\":\"\$http_x_forwarded_for\"")) shouldBe true
            (defaultConfTemplate.contains("\"request_time\":\$request_time")) shouldBe true
            (countOccurrences(defaultConfTemplate, "access_log /var/log/nginx/access.log {{ nginx_access_log_format_name }}")) shouldBe (2)
            (defaultConfTemplate.contains("proxy_set_header X-Request-ID \$request_id")) shouldBe true
            (infraReadme.contains("HTTP request completion logging은 nginx `access.log`가 소유한다")) shouldBe true
            (infraReadme.contains("application log는 business/domain event 중심")) shouldBe true
            (infraReadme.contains("app container(`apis`, `admin`, `batch`) run task에는 `ports:`를 추가하지 않는다")) shouldBe true
            (nginxUpdateScript.contains("BEAT MANAGED GENERATED UPSTREAM INCLUDES")) shouldBe true
            (nginxUpdateScript.contains("BEAT MANAGED GENERATED ROUTE INCLUDES")) shouldBe true
            (nginxUpdateScript.contains("bootstrap-includes")) shouldBe true
            (nginxUpdateScript.contains("upsert-upstream")) shouldBe true
            (nginxUpdateScript.contains("split-upstreams")) shouldBe false
            (nginxUpdateScript.contains("split_upstreams")) shouldBe false
            (nginxUpdateScript.contains("skip_existing")) shouldBe false
            (nginxUpdateScript.contains("json.dumps({\"changed\": changed})")) shouldBe true
            (nginxUpdateScript.contains("LOCK_DIR_ENV = \"BEAT_NGINX_LOCK_DIR\"")) shouldBe true
            (nginxUpdateScript.contains("import hashlib")) shouldBe true
            (nginxUpdateScript.contains("DEFAULT_LOCK_DIR = Path(\"/run/lock/beat-nginx\")")) shouldBe true
            (nginxUpdateScript.contains("lock_root.mkdir(parents=True, exist_ok=True)")) shouldBe true
            (nginxUpdateScript.contains("Path(tempfile.gettempdir()) / \"beat-nginx-locks\"")) shouldBe true
            (nginxUpdateScript.contains("def lock_filename(path: Path) -> str:")) shouldBe true
            (nginxUpdateScript.contains("lock_path = path.parent / (path.name + \".lock\")")) shouldBe false
            (deployPlaybook.contains("app_dev_switch")) shouldBe false
            (deployPlaybook.contains("app_prod_switch")) shouldBe false
            (deployPlaybook.contains("name: app_bluegreen")) shouldBe true
            (deployPlaybook.contains("tasks_from: run_switch.yml")) shouldBe true
            (deployPlaybook.contains("modules[module].deploy_mode == \"blue_green\"")) shouldBe true
            (deployPlaybook.contains("modules[module].deploy_mode == \"stop_start\"")) shouldBe true
            val postFailureRestoreValidationSuccess =
                "(app_bluegreen_post_failure_restore_validate_result.rc | default(1)) == 0"
            (appBluegreenRunSwitch.contains(postFailureRestoreValidationSuccess)) shouldBe true
            (deployPlaybook.contains("tags:")) shouldBe true
            (deployPlaybook.contains("- healthcheck")) shouldBe true
            (deployPlaybook.contains("- cleanup")) shouldBe true
            (rollbackPlaybook.contains("name: app_healthcheck")) shouldBe true
            (rollbackPlaybook.contains("module in modules")) shouldBe true
            (rollbackPlaybook.contains("name: Roll back runtime to previous release")) shouldBe true
            (rollbackPlaybook.contains("name: Execute rollback and restore metadata truth")) shouldBe true
            (rollbackPlaybook.contains("app_rollback_module_cfg.nginx_route is defined")) shouldBe true
            (rollbackPlaybook.contains("name: Restore stop-start current release after rollback failure")) shouldBe true
            (rollbackPlaybook.contains("app_stopstart_image: \"{{ app_rollback_current_release.image }}\"")) shouldBe true
            (rollbackPlaybook.contains("name: Healthcheck restored stop-start current release")) shouldBe true
            (rollbackPlaybook.contains("app_rollback_module_cfg.deploy_mode == 'stop_start'")) shouldBe true
            (rollbackPlaybook.contains("For stop-start modules, the playbook attempted to restore the archived current image")) shouldBe true
            assertBefore(rollbackPlaybook, "name: Roll back runtime to previous release", "name: Healthcheck module after rollback before metadata promotion")
            (rollbackPlaybook.contains("- rollback")) shouldBe true
            (appSecretRole.contains("application-secret.properties.j2")) shouldBe true
            (appSecretRole.contains("app_secret_content_normalized")) shouldBe false
            (deployPlaybook.contains("network_health_max_attempts: 30")) shouldBe true
            (rollbackPlaybook.contains("network_health_max_attempts: 30")) shouldBe true
            (deployDev.contains(".dockerignore")) shouldBe true
            (deployDev.contains("Build and push image")) shouldBe true
            (deployProd.contains("Build and push image")) shouldBe true
            (deployProd.contains("Validate release tag")) shouldBe true
            (deployProd.contains("resolve-release")) shouldBe true
            (deployProd.contains("release_tag")) shouldBe true
            (rollbackProd.contains("playbook: playbooks/rollback.yml")) shouldBe true
            // rollback-prod.yml delegates ref validation to the playbook itself; no in-workflow merge-base guard.
            (rollbackProd.contains("module: \${{ github.event.inputs.module }}")) shouldBe true
            (deployDev.contains("ansible-playbook playbooks/deploy.yml")) shouldBe false
            (deployProd.contains("ansible-playbook playbooks/deploy.yml")) shouldBe false
            (rollbackProd.contains("ansible-playbook playbooks/rollback.yml")) shouldBe false
            (deployDev.contains("Setup deploy tooling")) shouldBe false
            (deployProd.contains("Setup deploy tooling")) shouldBe false
            (deployDev.contains("inventory_sops_path:")) shouldBe false
            (deployProd.contains("inventory_sops_path:")) shouldBe false
            (rollbackProd.contains("inventory_sops_path:")) shouldBe false
            (deployDev.contains("inventory_label:")) shouldBe false
            (deployProd.contains("inventory_label:")) shouldBe false
            (rollbackProd.contains("inventory_label:")) shouldBe false
            (deployDev.contains("preferred_order = [\"admin\", \"apis\", \"batch\"]")) shouldBe true
            (deployDev.contains("modules = preferred_order if requested == \"all\" else [requested]")) shouldBe true
            (deployDev.contains("modules = [module for module in preferred_order if selected_modules[module]]")) shouldBe true
            (deployDev.contains("IMAGE_TAG=\"dev-\${RESOLVED_SHA}\"")) shouldBe true
            val devRuntimeImage =
                "image: \${{ vars.DEV_DOCKER_LOGIN_USERNAME }}/beat-\${{ matrix.module }}:dev-\${{ needs.resolve-ref.outputs.commit_sha }}"
            val prodRuntimeImage =
                "image: \${{ vars.PROD_DOCKER_LOGIN_USERNAME }}/beat-\${{ matrix.module }}:" +
                    "\${{ needs.resolve-release.outputs.release_tag }}"
            (deployDev.contains(devRuntimeImage)) shouldBe true
            (deployProd.contains("IMAGE_TAG=\"\${RELEASE_TAG}\"")) shouldBe true
            (deployProd.contains(prodRuntimeImage)) shouldBe true
            (appScriptsRole.contains("deploy-common.sh")) shouldBe false
            (appScriptsRole.contains("deploy-stop-start.sh")) shouldBe false
            (appScriptsRole.contains("Install repo-owned stop-start deployment helper")) shouldBe false
            (appScriptsRole.contains("deploy-blue-green.sh")) shouldBe false
            (appScriptsRole.contains("name: nginx_config_helper")) shouldBe true
            (appScriptsRole.contains("nginx_generated_source_dir")) shouldBe true
            (appScriptsRole.contains("nginx_generated_target_dir")) shouldBe true
            (appHealthcheckRole.contains("healthcheck_target_container")) shouldBe true
            (appHealthcheckRole.contains("healthcheck_target_container must be set")) shouldBe true
            (appHealthcheckRole.contains("include_tasks: probe.yml")) shouldBe true
            (appHealthcheckRole.contains("app_healthcheck_probe_target_container")) shouldBe true
            (appHealthcheckProbe.contains("app_healthcheck_probe_target_container")) shouldBe true
            (appHealthcheckProbe.contains("printf 'health attempt %s/%s failed")) shouldBe true
            (appHealthcheckProbe.contains("if [ \"\$attempt\" -lt \"\$attempts\" ]; then")) shouldBe true
            (appHealthcheckRole.contains("current-slot")) shouldBe false
            (appHealthcheckRole.contains("slurp:")) shouldBe false
            (appHealthcheckRole.contains("module_cfg.container_name | default(module)")) shouldBe false
            (appHealthcheckRole.contains("target=\"apis-\$slot\"")) shouldBe false
            (appCleanupRole.contains("docker_image_prune_retention | default('72h')")) shouldBe true
            (appCleanupRole.contains("docker_image_prune_failure_policy | default('warn') in ['warn', 'fail']")) shouldBe true
            (appCleanupRole.contains("docker_image_prune_failure_policy | default('warn') == 'fail'")) shouldBe true
            (appCleanupRole.contains("Total reclaimed space:\\\\s*0\\\\s*B")) shouldBe true
            (appCleanupRole.contains("app_cleanup_docker_prune_result.stdout | default('unknown error', true)")) shouldBe true
            (appCleanupRole.contains("until=72h")) shouldBe false
            (appCleanupRole.contains("failed_when: false")) shouldBe false
            (appRollbackRole.contains("app_rollback_archive_timestamp")) shouldBe true
            (appRollbackRole.contains("now(utc=true, fmt='%Y%m%dT%H%M%SZ')")) shouldBe true
            (appRollbackRole.contains("name: Read current release metadata before rollback")) shouldBe true
            (appRollbackRole.contains("app_rollback_current_release_raw")) shouldBe true
            (appRollbackRole.contains("app_rollback_current_release")) shouldBe true
            assertBefore(appRollbackRole, "name: Parse current release metadata before rollback", "name: Roll back stop-start lanes via Docker module")
            (appRollbackRole.contains("lookup('pipe', 'date -u")) shouldBe false
            (infraReadme.contains("Release metadata schema")) shouldBe true
            (infraReadme.contains("created_at`은 원격 EC2의 시스템 시간이 아니라 controller UTC")) shouldBe true
            (infraReadme.contains("SSH pipelining + sudo `requiretty` caveat")) shouldBe true
            (infraReadme.contains("Defaults requiretty")) shouldBe true
            (infraReadme.contains("Seed placeholder upstreams")) shouldBe true
            (infraReadme.contains("nginx_seed_placeholder_host:nginx_seed_placeholder_port")) shouldBe true
            (infraReadme.contains("127.0.0.1:65535")) shouldBe true
            (infraReadme.contains("community.docker.docker_volume_info")) shouldBe true
            (infraReadme.contains("nginx_legacy_config_volume_name")) shouldBe true
            (infraReadme.contains("/var/lib/docker/volumes")) shouldBe false
            (infraReadme.contains("Nginx fragment mapping contract")) shouldBe true
            (infraReadme.contains("nginx_fragments")) shouldBe true
            (infraReadme.contains("read-only contract")) shouldBe true
            (infraReadme.contains("Prod rollback rehearsal 절차")) shouldBe true
            (infraReadme.contains("legacyv1")) shouldBe true
            (infraReadme.contains("Restore stop-start current release after rollback failure")) shouldBe true
            (devInventory.contains("nginx_seed_placeholder_host: \"127.0.0.1\"")) shouldBe true
            (devInventory.contains("nginx_seed_placeholder_port: 65535")) shouldBe true
            (devInventory.contains("nginx_legacy_config_volume_name: nginx-config-volume")) shouldBe true
            (devInventory.contains("nginx_config_volume_name:")) shouldBe false
            (devInventory.contains("nginx_conf_target_path: /home/ubuntu/deployment/nginx/conf.d/default.conf")) shouldBe true
            (devInventory.contains("nginx_generated_source_dir: /home/ubuntu/deployment/nginx/generated-source")) shouldBe true
            (devInventory.contains("nginx_generated_target_dir: /home/ubuntu/deployment/nginx/generated")) shouldBe true
            (devInventory.contains("/var/lib/docker/volumes/nginx-config-volume/_data")) shouldBe false
            (devInventory.contains("nginx_fragments:")) shouldBe true
            (devInventory.contains("fragment_file: backend.conf")) shouldBe true
            (devInventory.contains("fragment_file: admin_backend.conf")) shouldBe true
            (devInventory.contains("fragment_file: actuator.conf")) shouldBe true
            (devInventory.contains("fragment_file: 10-managed.conf")) shouldBe true
            (prodInventory.contains("nginx_seed_placeholder_host: \"127.0.0.1\"")) shouldBe true
            (prodInventory.contains("nginx_seed_placeholder_port: 65535")) shouldBe true
            (prodInventory.contains("nginx_legacy_config_volume_name: nginx-config-volume")) shouldBe true
            (prodInventory.contains("nginx_config_volume_name:")) shouldBe false
            (prodInventory.contains("nginx_conf_target_path: /home/ubuntu/deployment/nginx/conf.d/default.conf")) shouldBe true
            (prodInventory.contains("nginx_generated_source_dir: /home/ubuntu/deployment/nginx/generated-source")) shouldBe true
            (prodInventory.contains("nginx_generated_target_dir: /home/ubuntu/deployment/nginx/generated")) shouldBe true
            (prodInventory.contains("/var/lib/docker/volumes/nginx-config-volume/_data")) shouldBe false
            (prodInventory.contains("nginx_fragments:")) shouldBe true
            (nginxBaseConfig.contains("nginx_base_config_transaction_operations")) shouldBe true
            (nginxBaseConfig.contains("sync-backend-upstream-target")) shouldBe true
            (nginxBaseConfig.contains("nginx_base_config_upstream_target_sync_result is defined")) shouldBe false
            (nginxBaseConfig.contains("nginx_seed_placeholder_host")) shouldBe true
            (nginxBaseConfig.contains("nginx_seed_placeholder_port")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragments.backend.fragment_file")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragments.admin.fragment_file")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragments.actuator.fragment_file")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragments.route.fragment_file")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragments.backend.upstream_name")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragments.admin.upstream_name")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragments.actuator.upstream_name")) shouldBe true
            (nginxBaseConfig.contains("- \"127.0.0.1\"\n" +
                "            - --backend-port\n" +
                "            - \"65535\"")) shouldBe false
            (nginxBaseConfig.contains("/upstreams/backend.conf\"")) shouldBe false
            (nginxBaseConfig.contains("/upstreams/admin_backend.conf\"")) shouldBe false
            (nginxBaseConfig.contains("/upstreams/actuator.conf\"")) shouldBe false
            (nginxBaseConfig.contains("/routes/10-managed.conf\"")) shouldBe false
            (appBluegreenRunSwitch.contains("nginx_fragments.backend.fragment_file")) shouldBe true
            (appBluegreenRunSwitch.contains("nginx_fragments.actuator.fragment_file")) shouldBe true
            (appBluegreenRunSwitch.contains("/upstreams/backend.conf\"")) shouldBe false
            (appBluegreenRunSwitch.contains("/upstreams/actuator.conf\"")) shouldBe false
            assertBefore(
                appBluegreenRunSwitch,
                "nginx_fragment_transaction_operations:",
                "nginx_fragment_transaction_failure_summary:")
            assertBefore(
                adminNginxRoute,
                "app_stopstart_admin_nginx_transaction_operations:",
                "nginx_fragment_transaction_failure_summary:")
            (adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_files:")) shouldBe true
            (adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_operations:")) shouldBe true
            (adminNginxRoute.contains("name: nginx_fragment_transaction")) shouldBe true
            (adminNginxRoute.contains("nginx_fragment_transaction_id: app-stopstart-admin-route")) shouldBe true
            (adminNginxRoute.contains(
                "nginx_fragment_transaction_files: \"{{ app_stopstart_admin_nginx_transaction_files }}\"")) shouldBe true
            (adminNginxRoute.contains(
                "nginx_fragment_transaction_operations: \"{{ app_stopstart_admin_nginx_transaction_operations }}\"")) shouldBe true
            (adminNginxRoute.contains("nginx_fragment_transaction_validate_command:")) shouldBe false
            (adminNginxRoute.contains("nginx_fragment_transaction_reload_command:")) shouldBe false
            (adminNginxRoute.contains("bootstrap-includes")) shouldBe true
            (adminNginxRoute.contains("split-upstreams")) shouldBe false
            (adminNginxRoute.contains("split_upstreams")) shouldBe false
            (adminNginxRoute.contains("upsert-upstream")) shouldBe true
            (adminNginxRoute.contains("ensure-route")) shouldBe true
            (adminNginxRoute.contains("sync-admin-upstream-target")) shouldBe true
            (adminNginxRoute.contains("sync-admin-route-target")) shouldBe true
            (adminNginxRoute.contains("nginx_fragments.admin.fragment_file")) shouldBe true
            (adminNginxRoute.contains("nginx_fragments.route.fragment_file")) shouldBe true
            (adminNginxRoute.contains("/upstreams/admin_backend.conf\"")) shouldBe false
            (adminNginxRoute.contains("/routes/10-managed.conf\"")) shouldBe false
            (adminNginxRoute.contains("backend-upstream-source")) shouldBe false
            (adminNginxRoute.contains("backend-upstream-target")) shouldBe false
            (adminNginxRoute.contains("actuator-upstream-source")) shouldBe false
            (adminNginxRoute.contains("actuator-upstream-target")) shouldBe false
            (adminNginxRoute.contains("tasks_from: migrate_legacy_upstreams.yml")) shouldBe false
            (adminNginxRoute.contains("legacy-upstream-source")) shouldBe false
            (adminNginxRoute.contains("legacy-upstream-target")) shouldBe false
            (adminNginxRoute.contains("verify-legacy-target-fragments")) shouldBe false
            (adminNginxRoute.contains("sync-legacy-backend-upstream-target")) shouldBe false
            (adminNginxRoute.contains("sync-legacy-admin-upstream-target")) shouldBe false
            (adminNginxRoute.contains("sync-legacy-actuator-upstream-target")) shouldBe false
            (adminNginxRoute.contains("remove-legacy-upstream-target-before-validation")) shouldBe false
            (adminNginxRoute.contains("Validate nginx config after admin route update")) shouldBe false
            (adminNginxRoute.contains("Reload nginx after admin route update")) shouldBe false
            (adminNginxRoute.contains("Backup current admin upstream source fragment")) shouldBe false
            (adminNginxRoute.contains("Restore previous live nginx source config")) shouldBe false
            (adminNginxRoute.contains("Remove admin nginx backup files after successful validation")) shouldBe false
            (adminNginxRoute.contains("/api/admin/")) shouldBe false
        }



            test("nginxFragmentTransactionOwnsBaseConfigValidateReloadAndRestoreBoundary") {
            val transaction = read("infra/ansible/roles/nginx_fragment_transaction/tasks/main.yml")
            val operation = read("infra/ansible/roles/nginx_fragment_transaction/tasks/operation.yml")
            val validateOperation = read("infra/ansible/roles/nginx_fragment_transaction/tasks/validate_operation.yml")
            val defaults = read("infra/ansible/roles/nginx_fragment_transaction/defaults/main.yml")
            val readme = read("infra/ansible/roles/nginx_fragment_transaction/README.md")
            val nginxBaseConfig = read("infra/ansible/roles/nginx_base_config/tasks/main.yml")
            val appBluegreenRunSwitch = read("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml")
            val adminNginxRoute = read("infra/ansible/roles/app_stopstart/tasks/admin_nginx_route.yml")

            (transaction.contains("scaffold-only")) shouldBe false
            (readme.contains("Current scaffold behavior")) shouldBe false
            (defaults.contains("nginx_fragment_transaction_files: []")) shouldBe true
            (defaults.contains("nginx_fragment_transaction_operations: []")) shouldBe true
            (transaction.contains("nginx_fragment_transaction_files")) shouldBe true
            (transaction.contains("nginx_fragment_transaction_operations")) shouldBe true
            (transaction.contains("block:")) shouldBe true
            (transaction.contains("rescue:")) shouldBe true
            (transaction.contains("remote_src: true")) shouldBe true
            (transaction.contains("nginx_fragment_transaction_file_pre_state: {}")) shouldBe true
            (transaction.contains("item.affects_reload is boolean")) shouldBe true
            (transaction.contains("mode: preserve")) shouldBe true
            (transaction.contains("nginx_fragment_transaction_file_pre_state.get(item.id, {})")) shouldBe true
            (transaction.contains("nginx_fragment_transaction_validate_command")) shouldBe true
            (transaction.contains("nginx_fragment_transaction_reload_command")) shouldBe true
            (transaction.contains("failed_when: false")) shouldBe true
            (transaction.contains("restore")) shouldBe true
            (transaction.contains("stdout")) shouldBe true
            (transaction.contains("stderr")) shouldBe true
            assertBefore(transaction, "nginx_fragment_transaction_validate_command", "nginx_fragment_transaction_reload_command")
            (validateOperation.contains("changed_if.stdout_json.changed is defined")) shouldBe true
            (validateOperation.contains("changed_if.stdout_contains is defined")) shouldBe true
            (operation.contains("nginx_fragment_transaction_command_stdout_json")) shouldBe true
            (operation.contains("from_json")) shouldBe true
            (operation.contains("__nginx_transaction_no_change_marker__")) shouldBe false
            (operation.contains("  when:\n" +
                "    - nginx_fragment_transaction_operation_should_run | bool\n" +
                "    - nginx_fragment_transaction_operation.kind == 'template'")) shouldBe true
            (operation.contains("  when:\n" +
                "    - nginx_fragment_transaction_operation_should_run | bool\n" +
                "    - nginx_fragment_transaction_operation.kind == 'command'")) shouldBe true
            (operation.contains("  when:\n" +
                "    - nginx_fragment_transaction_operation_should_run | bool\n" +
                "    - nginx_fragment_transaction_operation.kind == 'copy'")) shouldBe true
            (operation.contains("  when:\n" +
                "    - nginx_fragment_transaction_operation_should_run | bool\n" +
                "    - nginx_fragment_transaction_operation.kind == 'file_absent'")) shouldBe true

            (nginxBaseConfig.contains("name: nginx_fragment_transaction")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragment_transaction_id: nginx-base-config")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragment_transaction_files:")) shouldBe true
            (nginxBaseConfig.contains("nginx_fragment_transaction_operations:")) shouldBe true
            (nginxBaseConfig.contains("stdout_json:")) shouldBe true
            (nginxBaseConfig.contains("stdout_contains: changed=true")) shouldBe false
            (nginxBaseConfig.contains("src: \"{{ role_path }}/templates/default.conf.j2\"")) shouldBe true
            (nginxBaseConfig.contains("playbook_dir }}/../roles/nginx_base_config/templates/default.conf.j2")) shouldBe false
            (nginxBaseConfig.contains("Backup current upstream fragment source")) shouldBe false
            (nginxBaseConfig.contains("Backup current upstream fragment target")) shouldBe false
            (nginxBaseConfig.contains("Validate nginx config after base config update")) shouldBe false
            (nginxBaseConfig.contains("Reload nginx after base config update")) shouldBe false
            (nginxBaseConfig.contains("nginx_legacy_upstream_source_path")) shouldBe false
            (nginxBaseConfig.contains("nginx_legacy_upstream_target_path")) shouldBe false
            (nginxBaseConfig.contains("Abort unsafe legacy-target-only upstream migration")) shouldBe false
            val legacyTargetPlaceholderRefusal =
                "Refusing to replace legacy target upstream config with placeholder fragments."
            (nginxBaseConfig.contains(legacyTargetPlaceholderRefusal)) shouldBe false
            (nginxBaseConfig.contains("nginx_base_config_missing_upstream_sources")) shouldBe false
            (nginxBaseConfig.contains("legacy-upstream-source")) shouldBe false
            (nginxBaseConfig.contains("legacy-upstream-target")) shouldBe false
            (nginxBaseConfig.contains("split-legacy-upstream-source")) shouldBe false
            (nginxBaseConfig.contains("remove-legacy-upstream-target-before-validation")) shouldBe false
            (nginxBaseConfig.contains("when_file_missing: backend-upstream-source")) shouldBe true
            (readme.contains("nginx_base_config")) shouldBe true
            (readme.contains("nginx_fragment_transaction_file_pre_state")) shouldBe true
            (readme.contains("published file pre-state")) shouldBe true
            (readme.contains("changed_if.stdout_json.changed: true")) shouldBe true
            val bluegreenTransactionFilesVar =
                "nginx_fragment_transaction_files: \"{{ app_bluegreen_nginx_transaction_files }}\""
            val bluegreenTransactionOperationsVar =
                "nginx_fragment_transaction_operations: \"{{ app_bluegreen_nginx_transaction_operations }}\""
            val bluegreenTransactionBackupCleanupTask =
                "Remove blue-green nginx transaction backup files after successful rollout"
            (appBluegreenRunSwitch.contains("name: nginx_fragment_transaction")) shouldBe true
            (appBluegreenRunSwitch.contains("nginx_fragment_transaction_id: app-bluegreen-switch")) shouldBe true
            (appBluegreenRunSwitch.contains("app_bluegreen_nginx_transaction_files:")) shouldBe true
            (appBluegreenRunSwitch.contains(bluegreenTransactionFilesVar)) shouldBe true
            (appBluegreenRunSwitch.contains("nginx_fragment_transaction_files:")) shouldBe true
            (appBluegreenRunSwitch.contains("app_bluegreen_nginx_transaction_operations:")) shouldBe true
            (appBluegreenRunSwitch.contains(bluegreenTransactionOperationsVar)) shouldBe true
            (appBluegreenRunSwitch.contains("nginx_fragment_transaction_operations:")) shouldBe true
            (appBluegreenRunSwitch.contains("nginx_fragment_transaction_validate_command:")) shouldBe false
            (appBluegreenRunSwitch.contains("nginx_fragment_transaction_reload_command:")) shouldBe false
            (appBluegreenRunSwitch.contains("stdout_json:")) shouldBe true
            (appBluegreenRunSwitch.contains("stdout_contains: changed=true")) shouldBe false
            (appBluegreenRunSwitch.contains("split-upstreams")) shouldBe false
            (appBluegreenRunSwitch.contains("legacy-upstream-source")) shouldBe false
            (appBluegreenRunSwitch.contains("legacy-upstream-target")) shouldBe false
            (appBluegreenRunSwitch.contains("split-legacy-upstream-source")) shouldBe false
            (appBluegreenRunSwitch.contains("admin-upstream-source")) shouldBe false
            (appBluegreenRunSwitch.contains("admin-upstream-target")) shouldBe false
            (appBluegreenRunSwitch.contains("sync-admin-upstream-target-for-legacy-migration")) shouldBe false
            (appBluegreenRunSwitch.contains("remove-legacy-upstream-target-before-validation")) shouldBe false
            (appBluegreenRunSwitch.contains("upsert-upstream")) shouldBe true
            (appBluegreenRunSwitch.contains("Validate nginx config after upstream switch")) shouldBe false
            (appBluegreenRunSwitch.contains("Reload nginx after upstream switch")) shouldBe false
            (appBluegreenRunSwitch.contains("Restore previous nginx source config from backup")) shouldBe false
            (appBluegreenRunSwitch.contains("cleanup_backup_on_success: false")) shouldBe true
            (appBluegreenRunSwitch.contains("Restore blue-green nginx transaction files after failed rollout")) shouldBe true
            (appBluegreenRunSwitch.contains("Remove blue-green nginx transaction files absent before failed rollout")) shouldBe true
            (appBluegreenRunSwitch.contains(
                "Remove blue-green nginx transaction backup files after failed rollout restore")) shouldBe true
            (appBluegreenRunSwitch.contains("post_failure_restore_validate_rc=")) shouldBe true
            (appBluegreenRunSwitch.contains(bluegreenTransactionBackupCleanupTask)) shouldBe true
            (adminNginxRoute.contains("name: nginx_fragment_transaction")) shouldBe true
            (adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_files:")) shouldBe true
            (adminNginxRoute.contains(
                "nginx_fragment_transaction_files: \"{{ app_stopstart_admin_nginx_transaction_files }}\"")) shouldBe true
            (adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_operations:")) shouldBe true
            (adminNginxRoute.contains(
                "nginx_fragment_transaction_operations: \"{{ app_stopstart_admin_nginx_transaction_operations }}\"")) shouldBe true
            (adminNginxRoute.contains("nginx_fragment_transaction_validate_command:")) shouldBe false
            (adminNginxRoute.contains("nginx_fragment_transaction_reload_command:")) shouldBe false
            (adminNginxRoute.contains("stdout_json:")) shouldBe true
            (adminNginxRoute.contains("stdout_contains: changed=true")) shouldBe false
            (adminNginxRoute.contains("remove-legacy-upstream-target-before-validation")) shouldBe false
            (adminNginxRoute.contains("Validate nginx config after admin route update")) shouldBe false
            (adminNginxRoute.contains("Reload nginx after admin route update")) shouldBe false
            (adminNginxRoute.contains("Restore previous nginx source config from backup")) shouldBe false
        }


    }
}
