package com.beat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
	void repoOwnedDeploymentAutomationUsesSharedToolingAndModuleScans() throws Exception {
		String ciPr = read(".github/workflows/ci-pr.yml");
		String ansibleLintWorkflow = read(".github/workflows/ansible-lint.yml");
		String ansibleExecWorkflow = read(".github/workflows/_ansible-exec.yml");
		String trivyImageConfig = read(".trivy-image.yaml");
		String deployDev = read(".github/workflows/deploy-dev.yml");
		String deployProd = read(".github/workflows/deploy-prod.yml");
			String rollbackProd = read(".github/workflows/rollback-prod.yml");
			String setupDeployTooling = read(".github/actions/setup-deploy-tooling/action.yml");
			String setupAnsibleTooling = read(".github/actions/setup-ansible-tooling/action.yml");

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
		assertTrue(deployDev.contains("ssh_host: ${{ secrets.DEV_SSH_HOST }}"));
		assertTrue(deployProd.contains("ssh_host: ${{ secrets.PROD_SSH_HOST }}"));
		assertTrue(rollbackProd.contains("ssh_host: ${{ secrets.PROD_SSH_HOST }}"));
		assertTrue(deployDev.contains("ssh_host_fingerprint: ${{ secrets.DEV_SSH_HOST_FINGERPRINT }}"));
		assertTrue(deployProd.contains("ssh_host_fingerprint: ${{ secrets.PROD_SSH_HOST_FINGERPRINT }}"));
		assertTrue(rollbackProd.contains("ssh_host_fingerprint: ${{ secrets.PROD_SSH_HOST_FINGERPRINT }}"));
		assertTrue(deployDev.contains("ssh_private_key: ${{ secrets.DEV_SSH_PRIVATE_KEY }}"));
		assertTrue(deployProd.contains("ssh_private_key: ${{ secrets.PROD_SSH_PRIVATE_KEY }}"));
		assertTrue(rollbackProd.contains("ssh_private_key: ${{ secrets.PROD_SSH_PRIVATE_KEY }}"));
		assertTrue(ansibleExecWorkflow.contains("workflow_call:"));
		assertTrue(ansibleExecWorkflow.contains("checkout_ref:"));
		assertTrue(ansibleExecWorkflow.contains("environment: ${{ inputs.environment_name }}"));
		assertTrue(ansibleExecWorkflow.contains("setup-deploy-tooling"));
		assertTrue(ansibleExecWorkflow.contains("cmd=(ansible-playbook"));
		assertTrue(ansibleExecWorkflow.contains("EXTRA_VARS_PATH=\"$(mktemp /tmp/beat-extra-vars."));
		assertTrue(ansibleExecWorkflow.contains("echo \"EXTRA_VARS_PATH=$EXTRA_VARS_PATH\" >> \"$GITHUB_ENV\""));
		assertTrue(ansibleExecWorkflow.contains("cmd+=(--extra-vars \"@$EXTRA_VARS_PATH\")"));
		assertTrue(ansibleExecWorkflow.contains("Cleanup temporary credentials"));
		assertTrue(ansibleExecWorkflow.contains("Notify Slack (success)"));
		assertTrue(ansibleExecWorkflow.contains("ssh_host:"));
		assertTrue(ansibleExecWorkflow.contains("ssh_port:"));
		assertTrue(ansibleExecWorkflow.contains("ssh_host_fingerprint:"));
		assertTrue(ansibleExecWorkflow.contains("ssh_private_key:"));
		assertTrue(ansibleExecWorkflow.contains("sops_age_key:"));
		assertTrue(ansibleExecWorkflow.contains("slack_webhook_url:"));
		assertTrue(ansibleExecWorkflow.contains("SSH_HOST: ${{ inputs.ssh_host }}"));
		assertTrue(ansibleExecWorkflow.contains("SSH_PORT: ${{ inputs.ssh_port }}"));
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
			assertTrue(setupAnsibleTooling.contains("sigstore/cosign-installer@faadad0cce49287aee09b3a48701e75088a2c6ad"));
			assertTrue(setupAnsibleTooling.contains("Install verified age"));
			assertTrue(setupAnsibleTooling.contains("cosign verify-blob"));
			assertTrue(setupAnsibleTooling.contains("sha256sum -c"));
			assertTrue(setupAnsibleTooling.contains("ansible_core-2.17.14-py3-none-any.whl"));
		assertTrue(setupDeployTooling.contains("ssh-host-fingerprint"));
		assertTrue(setupDeployTooling.contains("ssh-keyscan -T 10"));
		assertTrue(setupDeployTooling.contains("ssh-keygen -lf - -E sha256"));
		assertTrue(setupDeployTooling.contains("Host fingerprint verification failed"));
		assertTrue(ansibleLintWorkflow.contains("ansible-lint"));
			assertTrue(ansibleLintWorkflow.contains("working-directory: infra/ansible"));
			assertTrue(ansibleLintWorkflow.contains("ansible-lint playbooks/*.yml roles"));
		assertTrue(ansibleLintWorkflow.contains(".github/workflows/_ansible-exec.yml"));
		assertTrue(deployDev.contains(".sops.yaml"));
		assertFalse(deployDev.contains(".sops.example.yaml"));
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
		String appHealthcheckRole = read("infra/ansible/roles/app_healthcheck/tasks/main.yml");
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
			assertTrue(Files.exists(Path.of("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py")));
			assertTrue(Files.exists(Path.of("infra/ansible/roles/foundation_stack/templates/foundation.compose.yml.j2")));
			assertTrue(Files.exists(Path.of("infra/ansible/roles/nginx_base_config/templates/default.conf.j2")));
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
		assertTrue(appBluegreenRunSwitch.contains("current-slot"));
		assertTrue(appBluegreenRunSwitch.contains("upsert-upstream"));
		assertTrue(appBluegreenRunSwitch.contains("public_smoke_url"));
		assertTrue(appBluegreenRunSwitch.contains("| combine({"));
		assertFalse(appBluegreenRunSwitch.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\""));
		assertTrue(appStopStartRole.contains("run_container.yml"));
		assertTrue(appStopStartRunContainer.contains("community.docker.docker_container"));
		assertTrue(appStopStartRunContainer.contains("BEAT_SCHEDULER_OWNER"));
		assertTrue(appStopStartRunContainer.contains("| combine({"));
		assertFalse(appStopStartRunContainer.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\""));
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
		assertTrue(deployPlaybook.contains("module_cfg.deploy_mode == 'blue_green'"));
		assertTrue(deployPlaybook.contains("module_cfg.deploy_mode == 'stop_start'"));
		assertTrue(deployPlaybook.contains("tags:"));
		assertTrue(deployPlaybook.contains("- healthcheck"));
		assertTrue(deployPlaybook.contains("- cleanup"));
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
		assertTrue(deployProd.contains("Validate version input"));
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
		assertTrue(deployDev.contains("IMAGE_TAG=\"dev-${GITHUB_SHA}\""));
		assertTrue(deployDev.contains("image: ${{ secrets.DEV_DOCKER_LOGIN_USERNAME }}/beat-${{ matrix.module }}:dev-${{ github.sha }}"));
		assertTrue(deployProd.contains("IMAGE_TAG=\"${VERSION}\""));
		assertTrue(deployProd.contains("image: ${{ secrets.PROD_DOCKER_LOGIN_USERNAME }}/beat-${{ github.event.inputs.module }}:${{ github.event.inputs.version }}"));
		assertFalse(appScriptsRole.contains("deploy-common.sh"));
		assertFalse(appScriptsRole.contains("deploy-stop-start.sh"));
		assertFalse(appScriptsRole.contains("Install repo-owned stop-start deployment helper"));
		assertFalse(appScriptsRole.contains("deploy-blue-green.sh"));
			assertTrue(appScriptsRole.contains("name: nginx_config_helper"));
		assertTrue(appScriptsRole.contains("nginx_generated_source_dir"));
		assertTrue(appScriptsRole.contains("nginx_generated_target_dir"));
		assertTrue(appHealthcheckRole.contains("module_cfg.container_name | default(module)"));
		assertTrue(appHealthcheckRole.contains("module_cfg.blue_container_name"));
		assertTrue(appHealthcheckRole.contains("module_cfg.green_container_name"));
		assertFalse(appHealthcheckRole.contains("target=\"apis-$slot\""));
		assertTrue(adminNginxRoute.contains("bootstrap-includes"));
		assertTrue(adminNginxRoute.contains("routes/10-managed.conf"));
		assertTrue(adminNginxRoute.contains("upstreams/00-managed.conf"));
		assertFalse(adminNginxRoute.contains("/api/admin/"));
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
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("letsencrypt_cert_name:"));
		assertFalse(read("infra/ansible/inventories/dev/group_vars/all/main.yml").contains("actuator_allow_cidrs:"));
		assertFalse(read("infra/ansible/inventories/dev/hosts.yml").contains("ansible_host:"));
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
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("letsencrypt_cert_name:"));
		assertFalse(read("infra/ansible/inventories/prod/group_vars/all/main.yml").contains("actuator_allow_cidrs:"));
		assertFalse(read("infra/ansible/inventories/prod/hosts.yml").contains("ansible_host:"));
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
}
