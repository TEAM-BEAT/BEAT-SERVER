package com.beat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RootRetirementContractTest {

	@Test
	void rootNoLongerOwnsExecutableRuntimeSources() {
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/BeatApplication.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/legacyroot/config/LegacyRootSecurityConfig.java")));
		assertFalse(
			Files.exists(Path.of("src/main/java/com/beat/global/common/handler/GlobalAsyncExceptionHandler.java")));
		assertFalse(Files.exists(
			Path.of("src/main/java/com/beat/global/common/scheduler/application/JobSchedulerService.java")));
		assertFalse(
			Files.exists(Path.of(
				"src/main/java/com/beat/global/common/scheduler/application/JobSchedulerTransactionalService.java")));
		assertFalse(
			Files.exists(Path.of("src/main/java/com/beat/domain/booking/application/TicketCleanupScheduler.java")));
		assertFalse(Files.exists(
			Path.of("src/main/java/com/beat/domain/promotion/application/PromotionSchedulerService.java")));
	}

	@Test
	void rootNoLongerOwnsApplicationResourceProfiles() {
		assertFalse(Files.exists(Path.of("src/main/resources/application.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/application-dev.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/application-local.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/application-prod.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/log4j2-spring.xml")));
	}

	@Test
	void concernOwnedApplicationResourcesExistAfterRootRetirement() {
		assertTrue(Files.exists(Path.of("observability/src/main/resources/application-observability.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-persistence.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-external.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-redis.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-thread-pool.yml")));
		assertTrue(Files.exists(Path.of("gateway/src/main/resources/application-jwt.yml")));
	}

	@Test
	void gradleBuildNoLongerVerifiesLegacyRootBootJarBaseline() throws Exception {
		String buildFile = Files.readString(Path.of("build.gradle.kts"));

		assertFalse(buildFile.contains("verifyLegacyV1Baseline"));
		assertTrue(buildFile.contains("verifyModuleBootJars"));
		assertTrue(buildFile.contains("\":apis:bootJar\""));
		assertTrue(buildFile.contains("\":admin:bootJar\""));
		assertTrue(buildFile.contains("\":batch:bootJar\""));
	}

	@Test
	void executableModuleBuildLogicOwnsLog4j2BackendAndObservabilityOwnsSharedConfig() throws Exception {
		String rootBuild = Files.readString(Path.of("build.gradle.kts"));
		String executableBuildLogic = Files.readString(
			Path.of("build-logic/src/main/kotlin/beat.spring-boot-app.gradle.kts"));

		assertFalse(rootBuild.contains("spring.boot.starter.log4j2"));
		assertFalse(rootBuild.contains("spring-boot-starter-logging"));
		assertTrue(executableBuildLogic.contains("spring-boot-starter-logging"));
		assertTrue(executableBuildLogic.contains("spring-boot-starter-log4j2"));
		assertTrue(executableBuildLogic.contains("tasks.withType<BootRun>().configureEach"));
		assertTrue(executableBuildLogic.contains("workingDir = rootDir"));
		assertTrue(Files.exists(Path.of("observability/src/main/resources/log4j2-spring.xml")));
	}

	@Test
	void batchRemainsTheSchedulerOwnerLaneAfterRootRetirement() throws Exception {
		Path schedulerService = Path.of(
			"batch/src/main/java/com/beat/batch/scheduler/application/JobSchedulerService.java");
		Path schedulerTransactionalService = Path.of(
			"batch/src/main/java/com/beat/batch/scheduler/application/JobSchedulerTransactionalService.java");
		Path ticketCleanupScheduler = Path.of(
			"batch/src/main/java/com/beat/batch/booking/application/TicketCleanupScheduler.java");
		Path promotionSchedulerService = Path.of(
			"batch/src/main/java/com/beat/batch/promotion/application/PromotionSchedulerService.java");

		assertTrue(Files.exists(schedulerService));
		assertTrue(Files.exists(schedulerTransactionalService));
		assertTrue(Files.exists(ticketCleanupScheduler));
		assertTrue(Files.exists(promotionSchedulerService));
		assertTrue(Files.readString(schedulerService).startsWith("package com.beat.batch.scheduler.application;"));
		assertTrue(Files.readString(schedulerTransactionalService)
			.startsWith("package com.beat.batch.scheduler.application;"));
		assertTrue(Files.readString(ticketCleanupScheduler).startsWith("package com.beat.batch.booking.application;"));
		assertTrue(
			Files.readString(promotionSchedulerService).startsWith("package com.beat.batch.promotion.application;"));
	}

	@Test
	void legacyDeploymentEntryPointsStayRetired() {
		assertFalse(Files.exists(Path.of(".github/workflows/dev-CI.yml")));
		assertFalse(Files.exists(Path.of(".github/workflows/prod-CI.yml")));
		assertFalse(Files.exists(Path.of("Dockerfile")));
		assertFalse(Files.exists(Path.of("Dockerfile-dev")));
		assertFalse(Files.exists(Path.of("Jenkinsfile")));
		assertFalse(Files.exists(Path.of(".github/workflows/v2-web-deploy-dev.yml")));
		assertFalse(Files.exists(Path.of(".github/workflows/v2-web-deploy-prod.yml")));
		assertFalse(Files.exists(Path.of("apis/src/main/java/com/beat/apis/user/api/HealthCheckController.java")));
		assertFalse(Files.exists(Path.of("apis/src/main/java/com/beat/apis/user/api/HealthCheckApi.java")));
	}

	@Test
	void devAndReusableDeploymentContractsUseSharedToolingAndInventoryOwnedSshMetadata() throws Exception {
		String ciPr = read(".github/workflows/ci-pr.yml");
		String ansibleLintWorkflow = read(".github/workflows/ansible-lint.yml");
		String ansibleExecWorkflow = read(".github/workflows/_ansible-exec.yml");
		String trivyImageConfig = read(".trivy-image.yaml");
			String deployDev = read(".github/workflows/deploy-dev.yml");
		String deployProd = read(".github/workflows/deploy-prod.yml");
		String rollbackProd = read(".github/workflows/rollback-prod.yml");
		String setupAnsibleTooling = read(".github/actions/setup-ansible-tooling/action.yml");
		String setupSshClient = read(".github/actions/setup-ssh-client/action.yml");
		String resolveAnsibleConnection = read(".github/actions/resolve-ansible-connection/action.yml");

		assertTrue(Files.exists(Path.of(".github/workflows/deploy-dev.yml")));
		assertTrue(Files.exists(Path.of(".github/workflows/deploy-prod.yml")));
		assertTrue(Files.exists(Path.of(".github/workflows/rollback-prod.yml")));
		assertTrue(Files.exists(Path.of(".github/workflows/_ansible-exec.yml")));
		assertTrue(Files.exists(Path.of(".github/workflows/ansible-lint.yml")));
		assertTrue(Files.exists(Path.of(".trivy-image.yaml")));
		assertTrue(ciPr.contains("verifyV2WebBaseline"));
		assertTrue(ciPr.contains("verifyModuleBootJars"));
		assertTrue(ciPr.contains("matrix:"));
		assertTrue(ciPr.contains("- apis"));
		assertTrue(ciPr.contains("- admin"));
		assertTrue(ciPr.contains("- batch"));
		assertTrue(ciPr.contains("MODULE=${{ matrix.module }}"));
		assertTrue(ciPr.contains("aquasecurity/trivy-action@57a97c7e7821a5776cebc9bb87c984fa69cba8f1"));
		assertTrue(ciPr.contains("scan-type: image"));
		assertTrue(ciPr.contains("trivy-config: .trivy-image.yaml"));
		assertTrue(trivyImageConfig.contains("ignore-unfixed: true"));
		assertTrue(trivyImageConfig.contains("vuln-type: os,library"));
		assertTrue(trivyImageConfig.contains("severity: CRITICAL,HIGH"));
		assertTrue(deployDev.contains("dorny/paths-filter"));
		assertTrue(deployDev.contains("fromJSON("));
		assertTrue(deployDev.contains("uses: ./.github/workflows/_ansible-exec.yml"));
		assertTrue(deployProd.contains("uses: ./.github/workflows/_ansible-exec.yml"));
		assertTrue(rollbackProd.contains("uses: ./.github/workflows/_ansible-exec.yml"));
			assertTrue(deployDev.contains("environment_name: dev"));
			assertTrue(deployProd.contains("environment_name: prod"));
			assertTrue(rollbackProd.contains("environment_name: prod"));
			assertFalse(deployDev.contains("secrets: inherit"));
			assertFalse(deployProd.contains("secrets: inherit"));
			assertFalse(rollbackProd.contains("secrets: inherit"));
			assertFalse(deployDev.contains("ssh_host: ${{"));
			assertFalse(deployProd.contains("ssh_host: ${{"));
			assertFalse(rollbackProd.contains("ssh_host: ${{"));
			assertFalse(deployDev.contains("ssh_host_fingerprint: ${{"));
			assertFalse(deployProd.contains("ssh_host_fingerprint: ${{"));
			assertFalse(rollbackProd.contains("ssh_host_fingerprint: ${{"));
			assertTrue(deployDev.contains("ssh_private_key: ${{ secrets.DEV_SSH_PRIVATE_KEY }}"));
			assertTrue(deployProd.contains("ssh_private_key: ${{ secrets.PROD_SSH_PRIVATE_KEY }}"));
			assertTrue(rollbackProd.contains("ssh_private_key: ${{ secrets.PROD_SSH_PRIVATE_KEY }}"));
			assertTrue(ansibleExecWorkflow.contains("workflow_call:"));
			assertTrue(ansibleExecWorkflow.contains("checkout_ref:"));
			assertTrue(ansibleExecWorkflow.contains("environment: ${{ inputs.environment_name }}"));
			assertTrue(ansibleExecWorkflow.contains("Setup Ansible tooling"));
			assertTrue(ansibleExecWorkflow.contains("resolve-ansible-connection"));
			assertTrue(ansibleExecWorkflow.contains("setup-ssh-client"));
			assertTrue(ansibleExecWorkflow.contains("cmd=(ansible-playbook"));
			assertTrue(ansibleExecWorkflow.contains("EXTRA_VARS_PATH=\"$(mktemp /tmp/beat-extra-vars."));
			assertTrue(ansibleExecWorkflow.contains("echo \"EXTRA_VARS_PATH=$EXTRA_VARS_PATH\" >> \"$GITHUB_ENV\""));
			assertTrue(ansibleExecWorkflow.contains("cmd+=(--extra-vars \"@$EXTRA_VARS_PATH\")"));
			assertTrue(ansibleExecWorkflow.contains("Cleanup temporary credentials"));
		assertTrue(ansibleExecWorkflow.contains("Notify Slack (success)"));
			assertFalse(ansibleExecWorkflow.contains("ssh_host:"));
			assertFalse(ansibleExecWorkflow.contains("ssh_port:"));
			assertFalse(ansibleExecWorkflow.contains("ssh_host_fingerprint:"));
			assertTrue(ansibleExecWorkflow.contains("ssh_private_key:"));
			assertTrue(ansibleExecWorkflow.contains("sops_age_key:"));
			assertTrue(ansibleExecWorkflow.contains("slack_webhook_url:"));
			assertFalse(ansibleExecWorkflow.contains("continue-on-error: true"));
			assertFalse(ansibleExecWorkflow.contains("LEGACY_HOST:"));
			assertFalse(ansibleExecWorkflow.contains("LEGACY_PORT:"));
			assertFalse(ansibleExecWorkflow.contains("SSH_CONNECTION_SOURCE"));
			assertTrue(ansibleExecWorkflow.contains("HAS_SLACK_WEBHOOK"));
			assertTrue(ansibleExecWorkflow.contains("ssh-private-key: ${{ secrets.ssh_private_key }}"));
			assertTrue(ansibleExecWorkflow.contains("SOPS_AGE_KEY: ${{ secrets.sops_age_key }}"));
			assertFalse(ansibleExecWorkflow.contains("ANSIBLE_SOPS_AGE_SSH_PRIVATE_KEYFILE"));
			assertFalse(ansibleExecWorkflow.contains("SOPS_AGE_SSH_PRIVATE_KEY_FILE=\"$HOME/.ssh/deploy_key\""));
			assertTrue(ansibleExecWorkflow.contains("SLACK_WEBHOOK_URL: ${{ secrets.slack_webhook_url }}"));
			assertTrue(ansibleExecWorkflow.contains("python3 - \"$EXTRA_VARS_PATH\" <<'PY'"));
		assertFalse(ansibleExecWorkflow.contains("inventory_sops_path:"));
		assertFalse(ansibleExecWorkflow.contains("INVENTORY_SOPS_PATH"));
		assertFalse(ansibleExecWorkflow.contains("inventory_label:"));
		assertFalse(ansibleExecWorkflow.contains("INVENTORY_LABEL"));
		assertFalse(ansibleExecWorkflow.contains("sops -d \"$INVENTORY_SOPS_PATH\""));
		assertFalse(ansibleExecWorkflow.contains(
			"inputs.environment_name == 'dev' && secrets.DEV_SSH_HOST || secrets.PROD_SSH_HOST"));
		assertFalse(ansibleExecWorkflow.contains("PROD_DOCKER_LOGIN_USERNAME"));
		assertTrue(setupAnsibleTooling.contains("sigstore/cosign-installer@cad07c2e89fa2edd6e2d7bab4c1aa38e53f76003"));
		assertTrue(setupAnsibleTooling.contains("Install verified age"));
		assertTrue(setupAnsibleTooling.contains("cosign verify-blob"));
		assertTrue(setupAnsibleTooling.contains("sha256sum -c"));
		assertTrue(setupAnsibleTooling.contains("ansible_core-2.17.14-py3-none-any.whl"));
		assertTrue(setupSshClient.contains("ssh-host-fingerprint"));
		assertTrue(setupSshClient.contains("ssh-keyscan -T 10"));
		assertTrue(setupSshClient.contains("ssh-keygen -lf - -E sha256"));
		assertTrue(setupSshClient.contains("Host fingerprint verification failed"));
		assertTrue(resolveAnsibleConnection.contains("Resolve SSH connection metadata"));
		assertFalse(resolveAnsibleConnection.contains("Setup Ansible tooling"));
		assertTrue(ansibleLintWorkflow.contains("ansible-lint"));
		assertTrue(ansibleLintWorkflow.contains("working-directory: infra/ansible"));
		assertTrue(ansibleLintWorkflow.contains("ansible-lint playbooks/*.yml roles"));
		assertTrue(ansibleLintWorkflow.contains(".github/workflows/_ansible-exec.yml"));
		assertTrue(deployDev.contains(".sops.yaml"));
		assertFalse(deployDev.contains(".sops.example.yaml"));
		}

	@Test
	void prodReleaseDeploymentUsesSharedImmutableVersionAndModuleMatrix() throws Exception {
		String deployProd = read(".github/workflows/deploy-prod.yml");
		String secretAwareVerify = read(".github/workflows/ansible-secret-aware-verify.yml");

		assertTrue(deployProd.contains("release:"));
		assertTrue(deployProd.contains("- published"));
		assertTrue(deployProd.contains("github.event.release.tag_name"));
		assertTrue(deployProd.contains("module_matrix"));
		assertTrue(deployProd.contains("matrix.module"));
		assertTrue(deployProd.contains("commit_sha"));
		assertTrue(deployProd.contains("ref: ${{ needs.resolve-release.outputs.commit_sha }}"));
		assertTrue(deployProd.contains("checkout_ref: ${{ needs.resolve-release.outputs.commit_sha }}"));
		assertFalse(deployProd.contains("workflow_dispatch:"));
		assertFalse(deployProd.contains("github.event.inputs.version"));
		assertFalse(deployProd.contains("github.event.inputs.module"));
		assertFalse(deployProd.contains("checkout_ref=refs/tags/"));
		assertTrue(deployProd.contains("module_matrix={\"include\":[{\"module\":\"admin\"},{\"module\":\"apis\"},{\"module\":\"batch\"}]}"));

		assertTrue(secretAwareVerify.contains("module: ${{ matrix.module }}"));
		assertTrue(secretAwareVerify.contains("Verified resolver for module=${MODULE}"));
		assertTrue(secretAwareVerify.contains("- admin"));
		assertTrue(secretAwareVerify.contains("- batch"));
	}

	@Test
	void nginxHelperSplitsLegacyManagedUpstreamsIntoOwnedFragments() throws Exception {
		Path script = Path.of("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py").toAbsolutePath();
		Path tempDir = Files.createTempDirectory("beat-nginx-upstreams-");
		Path upstreamDir = tempDir.resolve("upstreams");
		Path legacy = upstreamDir.resolve("00-managed.conf");
		Files.createDirectories(upstreamDir);
		Files.writeString(legacy, """
			# BEGIN BEAT MANAGED UPSTREAM backend
			upstream backend {
			    server apis-blue:4001;
			}
			# END BEAT MANAGED UPSTREAM backend

			# BEGIN BEAT MANAGED UPSTREAM admin_backend
			upstream admin_backend {
			    server admin:4000;
			}
			# END BEAT MANAGED UPSTREAM admin_backend

			# BEGIN BEAT MANAGED UPSTREAM actuator
			upstream actuator {
			    server apis-blue:9000;
			}
			# END BEAT MANAGED UPSTREAM actuator
			""");

		String firstRun = run(
			"python3",
			script.toString(),
			"split-upstreams",
			"--source",
			legacy.toString(),
			"--output-dir",
			upstreamDir.toString(),
			"--mapping",
			"backend=backend.conf",
			"--mapping",
			"admin_backend=admin_backend.conf",
			"--mapping",
			"actuator=actuator.conf",
			"--require-all");

		assertTrue(firstRun.contains("\"changed\": true"));
		assertTrue(Files.readString(upstreamDir.resolve("backend.conf")).contains("server apis-blue:4001;"));
		assertTrue(Files.readString(upstreamDir.resolve("admin_backend.conf")).contains("server admin:4000;"));
		assertTrue(Files.readString(upstreamDir.resolve("actuator.conf")).contains("server apis-blue:9000;"));

		String secondRun = run(
			"python3",
			script.toString(),
			"split-upstreams",
			"--source",
			legacy.toString(),
			"--output-dir",
			upstreamDir.toString(),
			"--mapping",
			"backend=backend.conf",
			"--mapping",
			"admin_backend=admin_backend.conf",
			"--mapping",
			"actuator=actuator.conf",
			"--require-all");

		assertTrue(secondRun.contains("\"changed\": false"));
	}

	@Test
	void nginxHelperFailsWhenExistingOwnedFragmentDiffersFromLegacyUpstream() throws Exception {
		Path script = Path.of("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py").toAbsolutePath();
		Path tempDir = Files.createTempDirectory("beat-nginx-upstream-preserve-");
		Path upstreamDir = tempDir.resolve("upstreams");
		Path legacy = upstreamDir.resolve("00-managed.conf");
		Files.createDirectories(upstreamDir);
		Files.writeString(upstreamDir.resolve("backend.conf"), """
			# BEGIN BEAT MANAGED UPSTREAM backend
			upstream backend {
			    server apis-live:4001;
			}
			# END BEAT MANAGED UPSTREAM backend
			""");
		Files.writeString(legacy, """
			# BEGIN BEAT MANAGED UPSTREAM backend
			upstream backend {
			    server apis-stale:4001;
			}
			# END BEAT MANAGED UPSTREAM backend

			# BEGIN BEAT MANAGED UPSTREAM admin_backend
			upstream admin_backend {
			    server admin-live:4000;
			}
			# END BEAT MANAGED UPSTREAM admin_backend

			# BEGIN BEAT MANAGED UPSTREAM actuator
			upstream actuator {
			    server apis-live:9000;
			}
			# END BEAT MANAGED UPSTREAM actuator
			""");

		Process process = new ProcessBuilder(
			"python3",
			script.toString(),
			"split-upstreams",
			"--source",
			legacy.toString(),
			"--output-dir",
			upstreamDir.toString(),
			"--mapping",
			"backend=backend.conf",
			"--mapping",
			"admin_backend=admin_backend.conf",
			"--mapping",
			"actuator=actuator.conf",
			"--require-all",
			"--skip-existing")
			.redirectErrorStream(true)
			.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exitCode = process.waitFor();

		assertTrue(exitCode != 0, output);
		assertTrue(output.contains("differs from legacy"));
		assertTrue(Files.readString(upstreamDir.resolve("backend.conf")).contains("server apis-live:4001;"));
		assertFalse(Files.readString(upstreamDir.resolve("backend.conf")).contains("server apis-stale:4001;"));
		assertFalse(Files.exists(upstreamDir.resolve("admin_backend.conf")));
		assertFalse(Files.exists(upstreamDir.resolve("actuator.conf")));
	}

	@Test
	void deploymentInfraUsesRepoOwnedHelpersAndConfiguredModuleContracts() throws Exception {
		String dockerfileModule = read("Dockerfile.module");
		String dockerignore = read(".dockerignore");
		String nginxUpdateScript = read("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py");
		String foundationPlaybook = read("infra/ansible/playbooks/foundation.yml");
		String foundationComposeTemplate = read("infra/ansible/roles/foundation_stack/templates/foundation.compose.yml.j2");
		String defaultConfTemplate = read("infra/ansible/roles/nginx_base_config/templates/default.conf.j2");
		String deployPlaybook = read("infra/ansible/playbooks/deploy.yml");
		String rollbackPlaybook = read("infra/ansible/playbooks/rollback.yml");
		String appSecretRole = read("infra/ansible/roles/app_secret/tasks/main.yml");
		String appScriptsRole = read("infra/ansible/roles/app_scripts/tasks/main.yml");
		String appBluegreenRunSwitch = read("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml");
		String appStopStartRole = read("infra/ansible/roles/app_stopstart/tasks/main.yml");
		String appStopStartRunContainer = read("infra/ansible/roles/app_stopstart/tasks/run_container.yml");
		String appContainerRuntimeEnv = read("infra/ansible/roles/app_container_runtime/tasks/env.yml");
		String appHealthcheckRole = read("infra/ansible/roles/app_healthcheck/tasks/main.yml");
		String appHealthcheckProbe = read("infra/ansible/roles/app_healthcheck/tasks/probe.yml");
		String appCleanupRole = read("infra/ansible/roles/app_cleanup/tasks/main.yml");
		String appRollbackRole = read("infra/ansible/roles/app_rollback/tasks/main.yml");
		String infraReadme = read("infra/README.md");
		String nginxBaseConfig = read("infra/ansible/roles/nginx_base_config/tasks/main.yml");
		String nginxLegacyMigration = read("infra/ansible/roles/nginx_config_helper/tasks/migrate_legacy_upstreams.yml");
		String adminNginxRoute = read("infra/ansible/roles/app_stopstart/tasks/admin_nginx_route.yml");
		String deployDev = read(".github/workflows/deploy-dev.yml");
		String deployProd = read(".github/workflows/deploy-prod.yml");
		String rollbackProd = read(".github/workflows/rollback-prod.yml");

		assertTrue(Files.exists(Path.of("infra/ansible/playbooks/deploy.yml")));
		assertTrue(Files.exists(Path.of("infra/ansible/playbooks/rollback.yml")));
		assertTrue(Files.exists(Path.of("infra/ansible/playbooks/foundation.yml")));
		assertTrue(Files.exists(Path.of("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml")));
		assertFalse(Files.exists(Path.of("infra/ansible/files/deploy-blue-green.sh")));
		assertFalse(Files.exists(Path.of("infra/ansible/files/deploy-stop-start.sh")));
		assertFalse(Files.exists(Path.of("infra/ansible/files/deploy-common.sh")));
		assertFalse(Files.exists(Path.of("infra/ansible/roles/app_dev_switch")));
		assertFalse(Files.exists(Path.of("infra/ansible/roles/app_prod_switch")));
		assertTrue(Files.exists(Path.of("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py")));
		assertTrue(Files.exists(Path.of("infra/ansible/roles/foundation_stack/templates/foundation.compose.yml.j2")));
		assertTrue(Files.exists(Path.of("infra/ansible/roles/nginx_base_config/templates/default.conf.j2")));
		assertTrue(Files.exists(Path.of("infra/ansible/roles/nginx_config_helper/tasks/migrate_legacy_upstreams.yml")));
		assertTrue(Files.exists(Path.of("scripts/generate-local-dev-secret.sh")));
		assertTrue(Files.exists(Path.of("scripts/generate-local-prod-secret.sh")));
		assertTrue(Files.exists(Path.of(".dockerignore")));
		assertTrue(dockerfileModule.contains("ARG MODULE"));
		assertTrue(dockerfileModule.contains("AS build"));
		assertTrue(dockerfileModule.contains(":${MODULE}:dependencies"));
		assertTrue(dockerfileModule.contains("cp \"$(find /workspace/${MODULE}/build/libs"));
		assertTrue(dockerfileModule.contains(
			"ENTRYPOINT [\"java\", \"-Duser.timezone=Asia/Seoul\", \"-jar\", \"/app/app.jar\"]"));
		assertFalse(dockerfileModule.contains("COPY src ./src"));
		assertFalse(dockerfileModule.contains("COPY --from=build /app/secret"));
		assertFalse(dockerfileModule.contains("SERVER_PORT"));
		assertFalse(dockerfileModule.contains("EXPOSE"));
		assertTrue(dockerignore.contains(".git"));
		assertTrue(dockerignore.contains("**/build"));
		assertTrue(dockerignore.contains(".omx"));
		assertTrue(dockerignore.contains("src/"));
		assertTrue(appBluegreenRunSwitch.contains("community.docker.docker_container"));
		assertTrue(appBluegreenRunSwitch.contains("name: nginx_fragment_transaction"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_files:"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_operations:"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_backend_upstream_source_path"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_backend_upstream_target_path"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_actuator_upstream_source_path"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_actuator_upstream_target_path"));
		assertFalse(appBluegreenRunSwitch.contains("Backup current nginx source config"));
		assertFalse(appBluegreenRunSwitch.contains("Backup current nginx target config"));
		assertFalse(appBluegreenRunSwitch.contains("Backup current managed upstream fragment"));
		assertTrue(appBluegreenRunSwitch.contains("current-slot"));
		assertTrue(appBluegreenRunSwitch.contains("upsert-upstream"));
		assertTrue(appBluegreenRunSwitch.contains("public_smoke_url"));
		assertTrue(appBluegreenRunSwitch.contains("app_container_env"));
		assertTrue(appBluegreenRunSwitch.contains("name: app_container_runtime"));
		assertTrue(appBluegreenRunSwitch.contains(
			"healthcheck_target_container: \"{{ app_bluegreen_target_container }}\""));
		assertTrue(appContainerRuntimeEnv.contains("| combine({"));
		assertTrue(appContainerRuntimeEnv.contains("'SPRING_PROFILES_ACTIVE': module_cfg.spring_profile"));
		assertFalse(appBluegreenRunSwitch.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\""));
		assertFalse(appBluegreenRunSwitch.contains("SPRING_PROFILES_ACTIVE"));
		assertTrue(appStopStartRole.contains("run_container.yml"));
		assertTrue(appStopStartRunContainer.contains("community.docker.docker_container"));
		assertTrue(appContainerRuntimeEnv.contains("BEAT_SCHEDULER_OWNER"));
		assertTrue(appStopStartRunContainer.contains("app_container_env"));
		assertTrue(appStopStartRunContainer.contains("name: app_container_runtime"));
		assertTrue(appStopStartRunContainer.contains(
			"healthcheck_target_container: \"{{ module_cfg.container_name | default(module) }}\""));
		assertTrue(appContainerRuntimeEnv.contains("| combine({"));
		assertFalse(appStopStartRunContainer.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\""));
		assertFalse(appStopStartRunContainer.contains("SPRING_PROFILES_ACTIVE"));
		assertTrue(foundationPlaybook.contains("role: foundation_stack"));
		assertTrue(foundationPlaybook.contains("role: nginx_base_config"));
		assertTrue(foundationComposeTemplate.contains("services:"));
		assertTrue(foundationComposeTemplate.contains("container_name: \"{{ nginx_container_name }}\""));
		assertTrue(foundationComposeTemplate.contains("foundation_mysql_enabled"));
		assertTrue(foundationComposeTemplate.contains("foundation_redis_enabled"));
		assertFalse(defaultConfTemplate.contains("upstream {{ backend_upstream_name"));
		assertFalse(defaultConfTemplate.contains("upstream {{ actuator_upstream_name"));
		assertTrue(defaultConfTemplate.contains("location {{ actuator_path }}/"));
		assertFalse(defaultConfTemplate.contains("location /admin/"));
		assertTrue(defaultConfTemplate.contains("BEAT MANAGED GENERATED UPSTREAM INCLUDES"));
		assertTrue(defaultConfTemplate.contains("BEAT MANAGED GENERATED ROUTE INCLUDES"));
		assertTrue(nginxUpdateScript.contains("BEAT MANAGED GENERATED UPSTREAM INCLUDES"));
		assertTrue(nginxUpdateScript.contains("BEAT MANAGED GENERATED ROUTE INCLUDES"));
		assertTrue(nginxUpdateScript.contains("bootstrap-includes"));
		assertTrue(nginxUpdateScript.contains("upsert-upstream"));
		assertTrue(nginxUpdateScript.contains("split-upstreams"));
		assertTrue(nginxUpdateScript.contains("skip_existing"));
		assertTrue(nginxUpdateScript.contains("json.dumps({\"changed\": changed})"));
		assertFalse(deployPlaybook.contains("app_dev_switch"));
		assertFalse(deployPlaybook.contains("app_prod_switch"));
		assertTrue(deployPlaybook.contains("role: app_bluegreen"));
		assertTrue(deployPlaybook.contains("tasks_from: run_switch.yml"));
		assertTrue(deployPlaybook.contains("module_cfg.deploy_mode == 'blue_green'"));
		assertTrue(deployPlaybook.contains("module_cfg.deploy_mode == 'stop_start'"));
		assertTrue(deployPlaybook.contains("tags:"));
		assertTrue(deployPlaybook.contains("- healthcheck"));
		assertTrue(deployPlaybook.contains("- cleanup"));
		assertTrue(rollbackPlaybook.contains("name: app_healthcheck"));
		assertTrue(rollbackPlaybook.contains("module in modules"));
		assertTrue(rollbackPlaybook.contains("module_cfg.nginx_route is defined"));
		assertTrue(rollbackPlaybook.contains("- rollback"));
		assertTrue(appSecretRole.contains("application-secret.properties.j2"));
		assertFalse(appSecretRole.contains("app_secret_content_normalized"));
		assertTrue(deployPlaybook.contains("network_health_max_attempts: 30"));
		assertTrue(rollbackPlaybook.contains("network_health_max_attempts: 30"));
		assertTrue(deployDev.contains(".dockerignore"));
		assertTrue(deployDev.contains("Build and push image"));
		assertTrue(deployProd.contains("Build and push image"));
		assertTrue(deployProd.contains("Validate release tag"));
		assertTrue(deployProd.contains("resolve-release"));
		assertTrue(deployProd.contains("release_tag"));
		assertTrue(rollbackProd.contains("playbook: playbooks/rollback.yml"));
		assertFalse(deployDev.contains("ansible-playbook playbooks/deploy.yml"));
		assertFalse(deployProd.contains("ansible-playbook playbooks/deploy.yml"));
		assertFalse(rollbackProd.contains("ansible-playbook playbooks/rollback.yml"));
		assertFalse(deployDev.contains("Setup deploy tooling"));
		assertFalse(deployProd.contains("Setup deploy tooling"));
		assertFalse(deployDev.contains("inventory_sops_path:"));
		assertFalse(deployProd.contains("inventory_sops_path:"));
		assertFalse(rollbackProd.contains("inventory_sops_path:"));
		assertFalse(deployDev.contains("inventory_label:"));
		assertFalse(deployProd.contains("inventory_label:"));
		assertFalse(rollbackProd.contains("inventory_label:"));
		assertTrue(deployDev.contains("preferred_order = [\"admin\", \"apis\", \"batch\"]"));
		assertTrue(deployDev.contains("modules = preferred_order if requested == \"all\" else [requested]"));
		assertTrue(deployDev.contains("modules = [module for module in preferred_order if selected_modules[module]]"));
		assertTrue(deployDev.contains("IMAGE_TAG=\"dev-${GITHUB_SHA}\""));
		assertTrue(deployDev.contains("image: ${{ vars.DEV_DOCKER_LOGIN_USERNAME }}/beat-${{ matrix.module }}:dev-${{ github.sha }}"));
		assertTrue(deployProd.contains("IMAGE_TAG=\"${RELEASE_TAG}\""));
		assertTrue(deployProd.contains("image: ${{ vars.PROD_DOCKER_LOGIN_USERNAME }}/beat-${{ matrix.module }}:${{ needs.resolve-release.outputs.release_tag }}"));
		assertFalse(appScriptsRole.contains("deploy-common.sh"));
		assertFalse(appScriptsRole.contains("deploy-stop-start.sh"));
		assertFalse(appScriptsRole.contains("Install repo-owned stop-start deployment helper"));
		assertFalse(appScriptsRole.contains("deploy-blue-green.sh"));
		assertTrue(appScriptsRole.contains("name: nginx_config_helper"));
		assertTrue(appScriptsRole.contains("nginx_generated_source_dir"));
		assertTrue(appScriptsRole.contains("nginx_generated_target_dir"));
		assertTrue(appHealthcheckRole.contains("healthcheck_target_container"));
		assertTrue(appHealthcheckRole.contains("healthcheck_target_container must be set"));
		assertTrue(appHealthcheckRole.contains("include_tasks: probe.yml"));
		assertTrue(appHealthcheckRole.contains("app_healthcheck_probe_target_container"));
		assertTrue(appHealthcheckProbe.contains("app_healthcheck_probe_target_container"));
		assertFalse(appHealthcheckRole.contains("current-slot"));
		assertFalse(appHealthcheckRole.contains("slurp:"));
		assertFalse(appHealthcheckRole.contains("module_cfg.container_name | default(module)"));
		assertFalse(appHealthcheckRole.contains("target=\"apis-$slot\""));
		assertTrue(appCleanupRole.contains("docker_image_prune_retention | default('72h')"));
		assertTrue(appCleanupRole.contains("docker_image_prune_failure_policy | default('warn') in ['warn', 'fail']"));
		assertTrue(appCleanupRole.contains("docker_image_prune_failure_policy | default('warn') == 'fail'"));
		assertFalse(appCleanupRole.contains("until=72h"));
		assertFalse(appCleanupRole.contains("failed_when: false"));
		assertTrue(appRollbackRole.contains("app_rollback_archive_timestamp"));
		assertTrue(appRollbackRole.contains("now(utc=true, fmt='%Y%m%dT%H%M%SZ')"));
		assertFalse(appRollbackRole.contains("lookup('pipe', 'date -u"));
		assertTrue(infraReadme.contains("Release metadata schema"));
		assertTrue(infraReadme.contains("created_at`은 원격 EC2의 시스템 시간이 아니라 controller UTC"));
		assertTrue(infraReadme.contains("SSH pipelining + sudo `requiretty` caveat"));
		assertTrue(infraReadme.contains("Defaults requiretty"));
		assertTrue(nginxBaseConfig.contains("nginx_base_config_transaction_operations"));
		assertTrue(nginxBaseConfig.contains("sync-backend-upstream-target"));
		assertFalse(nginxBaseConfig.contains("nginx_base_config_upstream_target_sync_result is defined"));
		assertTrue(nginxLegacyMigration.contains("00-managed.conf"));
		assertTrue(nginxLegacyMigration.contains("Sync split upstream fragments before removing legacy target"));
		assertTrue(nginxLegacyMigration.contains("Remove legacy upstream target before nginx validation"));
		assertBefore(nginxLegacyMigration, "Split legacy upstream source", "Check split upstream sources");
		assertBefore(nginxLegacyMigration, "Check split upstream sources", "Abort legacy upstream migration");
		assertBefore(nginxLegacyMigration, "Abort legacy upstream migration", "Sync split upstream fragments");
		assertBefore(nginxLegacyMigration, "Sync split upstream fragments", "Remove legacy upstream target");
		assertFalse(nginxLegacyMigration.contains("Remove legacy upstream source"));
		assertBefore(
			appBluegreenRunSwitch,
			"nginx_fragment_transaction_operations:",
			"nginx_fragment_transaction_validate_command:");
		assertBefore(
			adminNginxRoute,
			"app_stopstart_admin_nginx_transaction_operations:",
			"nginx_fragment_transaction_validate_command:");
		assertBefore(adminNginxRoute, "split-legacy-upstream-source", "remove-legacy-upstream-source");
		assertBefore(adminNginxRoute, "verify-legacy-target-fragments", "remove-legacy-upstream-target-before-validation");
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_files:"));
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_operations:"));
		assertTrue(adminNginxRoute.contains("name: nginx_fragment_transaction"));
		assertTrue(adminNginxRoute.contains("nginx_fragment_transaction_id: app-stopstart-admin-route"));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_files: \"{{ app_stopstart_admin_nginx_transaction_files }}\""));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_operations: \"{{ app_stopstart_admin_nginx_transaction_operations }}\""));
		assertTrue(adminNginxRoute.contains("nginx_fragment_transaction_validate_command:"));
		assertTrue(adminNginxRoute.contains("nginx_fragment_transaction_reload_command:"));
		assertTrue(adminNginxRoute.contains("bootstrap-includes"));
		assertTrue(adminNginxRoute.contains("split-upstreams"));
		assertTrue(adminNginxRoute.contains("upsert-upstream"));
		assertTrue(adminNginxRoute.contains("ensure-route"));
		assertTrue(adminNginxRoute.contains("sync-admin-upstream-target"));
		assertTrue(adminNginxRoute.contains("sync-admin-route-target"));
		assertTrue(adminNginxRoute.contains("routes/10-managed.conf"));
		assertTrue(adminNginxRoute.contains("upstreams/admin_backend.conf"));
		assertFalse(adminNginxRoute.contains("tasks_from: migrate_legacy_upstreams.yml"));
		assertFalse(adminNginxRoute.contains("Validate nginx config after admin route update"));
		assertFalse(adminNginxRoute.contains("Reload nginx after admin route update"));
		assertFalse(adminNginxRoute.contains("Backup current admin upstream source fragment"));
		assertFalse(adminNginxRoute.contains("Restore previous live nginx source config"));
		assertFalse(adminNginxRoute.contains("Remove admin nginx backup files after successful validation"));
		assertFalse(adminNginxRoute.contains("/api/admin/"));
	}

	@Test
	void nginxFragmentTransactionOwnsBaseConfigValidateReloadAndRestoreBoundary() throws Exception {
		String transaction = read("infra/ansible/roles/nginx_fragment_transaction/tasks/main.yml");
		String operation = read("infra/ansible/roles/nginx_fragment_transaction/tasks/operation.yml");
		String validateOperation = read("infra/ansible/roles/nginx_fragment_transaction/tasks/validate_operation.yml");
		String defaults = read("infra/ansible/roles/nginx_fragment_transaction/defaults/main.yml");
		String readme = read("infra/ansible/roles/nginx_fragment_transaction/README.md");
		String nginxBaseConfig = read("infra/ansible/roles/nginx_base_config/tasks/main.yml");
		String appBluegreenRunSwitch = read("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml");
		String adminNginxRoute = read("infra/ansible/roles/app_stopstart/tasks/admin_nginx_route.yml");

		assertFalse(transaction.contains("scaffold-only"));
		assertFalse(readme.contains("Current scaffold behavior"));
		assertTrue(defaults.contains("nginx_fragment_transaction_files: []"));
		assertTrue(defaults.contains("nginx_fragment_transaction_operations: []"));
		assertTrue(transaction.contains("nginx_fragment_transaction_files"));
		assertTrue(transaction.contains("nginx_fragment_transaction_operations"));
		assertTrue(transaction.contains("block:"));
		assertTrue(transaction.contains("rescue:"));
		assertTrue(transaction.contains("remote_src: true"));
		assertTrue(transaction.contains("nginx_fragment_transaction_validate_command"));
		assertTrue(transaction.contains("nginx_fragment_transaction_reload_command"));
		assertTrue(transaction.contains("failed_when: false"));
		assertTrue(transaction.contains("restore"));
		assertTrue(transaction.contains("stdout"));
		assertTrue(transaction.contains("stderr"));
		assertBefore(transaction, "nginx_fragment_transaction_validate_command", "nginx_fragment_transaction_reload_command");
		assertTrue(validateOperation.contains("changed_if.stdout_json.changed is defined"));
		assertTrue(validateOperation.contains("changed_if.stdout_contains is defined"));
		assertTrue(operation.contains("nginx_fragment_transaction_command_stdout_json"));
		assertTrue(operation.contains("from_json"));
		assertFalse(operation.contains("__nginx_transaction_no_change_marker__"));
		assertTrue(operation.contains("  when:\n"
			+ "    - nginx_fragment_transaction_operation_should_run | bool\n"
			+ "    - nginx_fragment_transaction_operation.kind == 'template'"));
		assertTrue(operation.contains("  when:\n"
			+ "    - nginx_fragment_transaction_operation_should_run | bool\n"
			+ "    - nginx_fragment_transaction_operation.kind == 'command'"));
		assertTrue(operation.contains("  when:\n"
			+ "    - nginx_fragment_transaction_operation_should_run | bool\n"
			+ "    - nginx_fragment_transaction_operation.kind == 'copy'"));
		assertTrue(operation.contains("  when:\n"
			+ "    - nginx_fragment_transaction_operation_should_run | bool\n"
			+ "    - nginx_fragment_transaction_operation.kind == 'file_absent'"));

		assertTrue(nginxBaseConfig.contains("name: nginx_fragment_transaction"));
		assertTrue(nginxBaseConfig.contains("nginx_fragment_transaction_id: nginx-base-config"));
		assertTrue(nginxBaseConfig.contains("nginx_fragment_transaction_files:"));
		assertTrue(nginxBaseConfig.contains("nginx_fragment_transaction_operations:"));
		assertTrue(nginxBaseConfig.contains("stdout_json:"));
		assertFalse(nginxBaseConfig.contains("stdout_contains: changed=true"));
		assertTrue(nginxBaseConfig.contains("src: \"{{ role_path }}/templates/default.conf.j2\""));
		assertFalse(nginxBaseConfig.contains("playbook_dir }}/../roles/nginx_base_config/templates/default.conf.j2"));
		assertFalse(nginxBaseConfig.contains("Backup current upstream fragment source"));
		assertFalse(nginxBaseConfig.contains("Backup current upstream fragment target"));
		assertFalse(nginxBaseConfig.contains("Validate nginx config after base config update"));
		assertFalse(nginxBaseConfig.contains("Reload nginx after base config update"));
		assertTrue(nginxBaseConfig.contains("Abort unsafe legacy-target-only upstream migration"));
		assertTrue(nginxBaseConfig.contains("Refusing to replace legacy target upstream config with placeholder fragments."));
		assertTrue(nginxBaseConfig.contains("nginx_base_config_missing_upstream_sources | length > 0"));
		assertTrue(nginxBaseConfig.contains("Remove legacy upstream target before nginx validation"));
		assertTrue(nginxBaseConfig.contains("when_file_missing: backend-upstream-source"));
		assertTrue(readme.contains("nginx_base_config"));
		assertTrue(readme.contains("nginx_fragment_transaction_file_pre_state"));
		assertTrue(readme.contains("published file pre-state"));
		assertTrue(readme.contains("changed_if.stdout_json.changed: true"));
		assertTrue(appBluegreenRunSwitch.contains("name: nginx_fragment_transaction"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_id: app-bluegreen-switch"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_nginx_transaction_files:"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_files: \"{{ app_bluegreen_nginx_transaction_files }}\""));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_files:"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_nginx_transaction_operations:"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_operations: \"{{ app_bluegreen_nginx_transaction_operations }}\""));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_operations:"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_validate_command:"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_reload_command:"));
		assertTrue(appBluegreenRunSwitch.contains("stdout_json:"));
		assertFalse(appBluegreenRunSwitch.contains("stdout_contains: changed=true"));
		assertTrue(appBluegreenRunSwitch.contains("upsert-upstream"));
		assertFalse(appBluegreenRunSwitch.contains("Validate nginx config after upstream switch"));
		assertFalse(appBluegreenRunSwitch.contains("Reload nginx after upstream switch"));
		assertFalse(appBluegreenRunSwitch.contains("Restore previous nginx source config from backup"));
		assertTrue(appBluegreenRunSwitch.contains("cleanup_backup_on_success: false"));
		assertTrue(appBluegreenRunSwitch.contains("Restore blue-green nginx transaction files after failed rollout"));
		assertTrue(appBluegreenRunSwitch.contains("Remove blue-green nginx transaction files absent before failed rollout"));
		assertTrue(appBluegreenRunSwitch.contains("post_failure_restore_validate_rc="));
		assertTrue(appBluegreenRunSwitch.contains("Remove blue-green nginx transaction backup files after successful rollout"));
		assertTrue(adminNginxRoute.contains("name: nginx_fragment_transaction"));
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_files:"));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_files: \"{{ app_stopstart_admin_nginx_transaction_files }}\""));
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_operations:"));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_operations: \"{{ app_stopstart_admin_nginx_transaction_operations }}\""));
		assertTrue(adminNginxRoute.contains("nginx_fragment_transaction_validate_command:"));
		assertTrue(adminNginxRoute.contains("nginx_fragment_transaction_reload_command:"));
		assertTrue(adminNginxRoute.contains("stdout_json:"));
		assertFalse(adminNginxRoute.contains("stdout_contains: changed=true"));
		assertTrue(adminNginxRoute.contains("remove-legacy-upstream-target-before-validation"));
		assertFalse(adminNginxRoute.contains("Validate nginx config after admin route update"));
		assertFalse(adminNginxRoute.contains("Reload nginx after admin route update"));
		assertFalse(adminNginxRoute.contains("Restore previous nginx source config from backup"));
	}

	@Test
	void inventoryAndSecurityConfigsOwnEnvironmentSpecificHealthContracts() throws Exception {
		String localDevSecretScript = read("scripts/generate-local-dev-secret.sh");
		String localProdSecretScript = read("scripts/generate-local-prod-secret.sh");
		String localVarsHelper = read("scripts/lib/local-vars.sh");
		String apisSecurity = read("apis/src/main/java/com/beat/apis/config/ApisSecurityConfig.java");
		String adminSecurity = read("admin/src/main/java/com/beat/admin/config/AdminSecurityConfig.java");
		String ansibleConfig = read("infra/ansible/ansible.cfg");
		String sopsConfig = read(".sops.yaml");

		assertTrue(Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all/main.yml")));
		assertTrue(Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml")));
		assertTrue(Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all/main.yml")));
		assertTrue(Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml")));
		assertFalse(Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all.sops.yml")));
		assertFalse(Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all.sops.yml")));
		assertFalse(Files.exists(Path.of("infra/ansible/inventories/dev/group_vars/all.sops.example.yml")));
		assertFalse(Files.exists(Path.of("infra/ansible/inventories/prod/group_vars/all.sops.example.yml")));
		assertFalse(Files.exists(Path.of(".sops.example.yaml")));
		assertTrue(Files.exists(Path.of(".sops.yaml")));
		assertTrue(Files.exists(Path.of("scripts/lib/local-vars.sh")));
		assertTrue(sopsConfig.contains("group_vars/all/.*\\.sops\\.yml"));
		assertFalse(sopsConfig.contains("age1replacewithdevrecipientkey"));
		assertTrue(read(".github/workflows/deploy-dev.yml").contains(".sops.yaml"));
		assertTrue(ansibleConfig.contains("vars_plugins_enabled = host_group_vars,community.sops.sops"));
		assertTrue(ansibleConfig.contains("vars_stage = inventory"));
		assertFalse(ansibleConfig.contains("age_ssh_private_keyfile = ~/.ssh/beat-dev"));
		assertTrue(localDevSecretScript.contains("DEV_ACTUATOR_PORT"));
		assertTrue(localDevSecretScript.contains("DEV_ACTUATOR_PATH"));
		assertTrue(localDevSecretScript.contains("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml"));
		assertFalse(localDevSecretScript.contains("all.local.sops.yml"));
		assertFalse(localDevSecretScript.contains("all.sops.example.yml"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_port:"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_path:"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_upstream_port:"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_public_path:"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("nginx_server_name:"));
		assertTrue(read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("nginx_server_name:"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("letsencrypt_cert_name:"));
		assertTrue(read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("letsencrypt_cert_name:"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_allow_cidrs:"));
		assertTrue(read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("actuator_allow_cidrs:"));
		assertFalse(read("infra/ansible/inventories/dev/hosts.yml").contains("ansible_host:"));
		assertTrue(read("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml").contains("ansible_host:"));
		assertTrue(localDevSecretScript.contains("sops -d --extract '[\"actuator_port\"]'"));
		assertTrue(localProdSecretScript.contains("PROD_ACTUATOR_PORT"));
		assertTrue(localProdSecretScript.contains("PROD_ACTUATOR_PATH"));
		assertTrue(localProdSecretScript.contains("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml"));
		assertFalse(localProdSecretScript.contains("all.local.sops.yml"));
		assertFalse(localProdSecretScript.contains("all.sops.example.yml"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_port:"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_path:"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_upstream_port:"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_public_path:"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("nginx_server_name:"));
		assertTrue(read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("nginx_server_name:"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("letsencrypt_cert_name:"));
		assertTrue(read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("letsencrypt_cert_name:"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_allow_cidrs:"));
		assertTrue(read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("actuator_allow_cidrs:"));
		assertFalse(read("infra/ansible/inventories/prod/hosts.yml").contains("ansible_host:"));
		assertTrue(read("infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml").contains("ansible_host:"));
		assertTrue(localProdSecretScript.contains("sops -d --extract '[\"actuator_port\"]'"));
		assertTrue(localVarsHelper.contains("require_sops_identity"));
		assertFalse(localVarsHelper.contains("read_yaml_value"));
		assertFalse(localVarsHelper.contains("$HOME/.ssh/beat-dev"));
		assertFalse(ansibleConfig.contains("host_key_checking = False"));
		assertFalse(ansibleConfig.contains("StrictHostKeyChecking=no"));
		assertFalse(apisSecurity.contains("\"/health-check\""));
		assertFalse(adminSecurity.contains("\"/health-check\""));
		assertFalse(apisSecurity.contains("\"/actuator/health\""));
		assertFalse(adminSecurity.contains("\"/actuator/health\""));
		assertTrue(apisSecurity.contains("actuatorEndPoint + \"/health\""));
		assertTrue(adminSecurity.contains("actuatorEndPoint + \"/health\""));
	}

	private static String read(String path) throws IOException {
		return Files.readString(Path.of(path));
	}

	private static String run(String... command) throws Exception {
		Process process = new ProcessBuilder(command)
			.redirectErrorStream(true)
			.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exitCode = process.waitFor();
		assertEquals(0, exitCode, output);
		return output;
	}

	private static void assertBefore(String content, String first, String second) {
		int firstIndex = content.indexOf(first);
		int secondIndex = content.indexOf(second);
		assertTrue(firstIndex >= 0, first);
		assertTrue(secondIndex >= 0, second);
		assertTrue(firstIndex < secondIndex, first + " should appear before " + second);
	}
}
