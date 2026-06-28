package com.beat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RootRetirementContractTest {

	@Test
	void rootNoLongerOwnsExecutableRuntimeSources() {
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/BeatApplication.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/legacyroot/config/LegacyRootSecurityConfig.java")));
		assertFalse(
			Files.exists(Path.of("src/main/java/com/beat/global/support/handler/GlobalAsyncExceptionHandler.java")));
		assertFalse(Files.exists(
			Path.of("src/main/java/com/beat/global/support/scheduler/application/JobSchedulerService.java")));
		assertFalse(
			Files.exists(Path.of(
				"src/main/java/com/beat/global/support/scheduler/application/JobSchedulerTransactionalService.java")));
		assertFalse(
			Files.exists(Path.of("src/main/java/com/beat/domain/booking/application/TicketCleanupService.java")));
		assertFalse(
			Files.exists(Path.of("src/main/java/com/beat/domain/booking/application/TicketCleanupScheduler.java")));
		assertFalse(Files.exists(
			Path.of("src/main/java/com/beat/domain/promotion/application/PromotionMaintenanceService.java")));
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
	void persistenceProfileKeepsProdSqlLoggingOffWhileDevSqlLoggingStaysExplicitOptIn() throws Exception {
		String persistence = read("infra/src/main/resources/application-persistence.yml");
		String baseSection = persistence.substring(0, persistence.indexOf("\n---"));
		String prodSection = sectionAfter(persistence, "on-profile: prod");
		String devSection = sectionAfter(persistence, "on-profile: dev");

		assertTrue(devSection.contains("show-sql: true"));
		assertTrue(baseSection.contains("format_sql: true"));
		assertTrue(prodSection.contains("show-sql: false"));
		assertTrue(prodSection.contains("format_sql: false"));
		assertTrue(prodSection.contains("org.hibernate.SQL: WARN"));
		assertTrue(prodSection.contains("org.hibernate.orm.jdbc.bind: WARN"));
		assertFalse(prodSection.contains("org.hibernate.SQL: DEBUG"));
		assertFalse(prodSection.contains("org.hibernate.SQL: TRACE"));
		assertFalse(prodSection.contains("org.hibernate.orm.jdbc.bind: DEBUG"));
		assertFalse(prodSection.contains("org.hibernate.orm.jdbc.bind: TRACE"));
	}

	@Test
	void domainInvariantTestsLiveWithDomainModule() {
		assertFalse(Files.exists(Path.of("src/test/java/com/beat/domain")));
		assertTrue(Files.exists(Path.of("domain/src/test/java/com/beat/domain")));
	}

	@Test
	void gradleBuildNoLongerVerifiesLegacyRootBootJarBaseline() throws Exception {
		String buildFile = Files.readString(Path.of("build.gradle.kts"));

		assertFalse(buildFile.contains("implementation(project(\":module-contracts\"))"));
		assertFalse(buildFile.contains("implementation(project(\":global-support\"))"));
		assertFalse(buildFile.contains("implementation(project(\":gateway\"))"));
		assertFalse(buildFile.contains("implementation(project(\":observability\"))"));
		assertFalse(buildFile.contains("implementation(project(\":infra\"))"));
		assertFalse(buildFile.contains("implementation(project(\":domain\"))"));
		assertFalse(buildFile.contains("testImplementation(project(\":domain\"))"));
		assertFalse(buildFile.contains("runtimeOnly(libs.mysql.connector.j)"));
		assertFalse(buildFile.contains("runtimeOnly(libs.jjwt.impl)"));
		assertFalse(buildFile.contains("runtimeOnly(libs.jjwt.jackson)"));
		assertFalse(buildFile.contains("compileOnly {"));
		assertFalse(buildFile.contains("annotationProcessor"));
		assertFalse(buildFile.contains("testImplementation(libs.bundles.test.common)"));
		assertFalse(buildFile.contains("testImplementation(libs.bundles.integration.testcontainers)"));
		assertFalse(buildFile.contains("libs.spring.boot.starter"));
		assertFalse(buildFile.contains("libs.spring.cloud"));
		assertFalse(buildFile.contains("libs.awspring"));
		assertFalse(buildFile.contains("libs.aws.java.sdk"));
		assertFalse(buildFile.contains("libs.querydsl"));
		assertFalse(buildFile.contains("verifyLegacyV1Baseline"));
		assertFalse(buildFile.contains("verifyV2WebBaseline"));
		assertTrue(buildFile.contains("testImplementation(libs.junit.jupiter)"));
		assertTrue(buildFile.contains("Builds the non-executable root coordination artifact."));
		assertTrue(buildFile.contains("verifyModuleBootJars"));
		assertTrue(buildFile.contains("\":apis:bootJar\""));
		assertTrue(buildFile.contains("\":admin:bootJar\""));
		assertTrue(buildFile.contains("\":batch:bootJar\""));
	}

	@Test
	void rootCoordinationBuildNoLongerAppliesExecutableOrKotlinPlugins() throws Exception {
		String buildFile = Files.readString(Path.of("build.gradle.kts"));

		assertTrue(buildFile.contains("java"));
		assertTrue(buildFile.contains("alias(libs.plugins.sonarqube)"));
		assertTrue(buildFile.contains("alias(libs.plugins.kover)"));
		assertTrue(buildFile.contains("id(\"beat.test\")"));

		assertFalse(buildFile.contains("alias(libs.plugins.sentry.jvm) apply false"));
		assertFalse(buildFile.contains("SentryPluginExtension"));
		assertFalse(buildFile.contains("sentrySdkVersion"));
		assertFalse(buildFile.contains("io.sentry.jvm.gradle"));
		assertFalse(buildFile.contains("collectExternalDependenciesForSentry"));
		assertFalse(buildFile.contains("generateSentry"));

		assertFalse(buildFile.contains("alias(libs.plugins.spring.boot)"));
		assertFalse(buildFile.contains("alias(libs.plugins.spring.dependency.management)"));
		assertFalse(buildFile.contains("alias(libs.plugins.kotlin.jvm)"));
		assertFalse(buildFile.contains("alias(libs.plugins.kotlin.spring)"));
		assertFalse(buildFile.contains("alias(libs.plugins.kotlin.jpa)"));
		assertFalse(buildFile.contains("tasks.named(\"bootJar\")"));
		assertFalse(buildFile.contains("tasks.named(\"bootRun\")"));
		assertFalse(buildFile.contains("tasks.withType<KotlinCompile>()"));
		assertFalse(buildFile.contains("queryDslSrcDir"));
		assertFalse(buildFile.contains("generatedSourceOutputDirectory"));
		assertFalse(buildFile.contains("generated/querydsl"));
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
	void observabilityOwnsApplicationLogMdcKeyValueAndRoutePatternContract() throws Exception {
		String log4j2 = read("observability/src/main/resources/log4j2-spring.xml");
		String baseFilter = read("observability/src/main/kotlin/com/beat/observability/logging/filter/BaseMdcLoggingFilter.kt");
		String routeInterceptor =
			read("observability/src/main/kotlin/com/beat/observability/logging/interceptor/RoutePatternMdcInterceptor.kt");
		String loggingConfig = read("observability/src/main/kotlin/com/beat/observability/logging/LoggingConfig.kt");
		String observabilityReadme = read("observability/README.md");

		assertTrue(log4j2.contains("[traceId=%equals{%X{traceId}}{}{NO_TRACE}]"));
		assertTrue(log4j2.contains("[userId=%equals{%X{userId}}{}{GUEST}]"));
		assertTrue(log4j2.contains("[clientIp=%equals{%X{clientIp}}{}{UNKNOWN}]"));
		assertTrue(log4j2.contains("[request=%equals{%X{requestInfo}}{}{NO_REQUEST}]"));
		assertTrue(log4j2.contains("[route=%equals{%X{routePattern}}{}{NO_ROUTE}]"));
		assertFalse(log4j2.contains("[%equals{%X{traceId}}{}{NO_TRACE}]"));
		assertTrue(baseFilter.contains("const val ROUTE_PATTERN_KEY = \"routePattern\""));
		assertTrue(routeInterceptor.contains("HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE"));
		assertTrue(routeInterceptor.contains("MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY"));
		assertTrue(routeInterceptor.contains("\"${request.method} $bestMatchingPattern\""));
		// The interceptor intentionally does NOT call MDC.remove — BaseMdcLoggingFilter.doFilterInternal
		// finally owns all MDC cleanup (including routePattern) so the access log (emitted in filter
		// finally, which runs AFTER interceptor afterCompletion) can read routePattern directly.
		assertFalse(routeInterceptor.contains("MDC.remove(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY)"));
		assertTrue(loggingConfig.contains("registry.addInterceptor(routePatternMdcInterceptor)"));
		assertTrue(observabilityReadme.contains("Request completion logging은 nginx `access.log`가 소유합니다"));
		assertTrue(observabilityReadme.contains("Application log는 request completion log를 중복으로 남기지 않고"));
		assertTrue(observabilityReadme.contains("route-level aggregation은 가능한 경우 `routePattern`을 사용합니다"));
	}

	@Test
	void nginxBaseConfigOwnsConservativeScannerBlockContract() throws Exception {
		String defaultConfTemplate = read("infra/ansible/roles/nginx_base_config/templates/default.conf.j2");
		String defaults = read("infra/ansible/roles/nginx_base_config/defaults/main.yml");
		String tasks = read("infra/ansible/roles/nginx_base_config/tasks/main.yml");
		String infraReadme = read("infra/README.md");
		String httpServer = sectionBetween(defaultConfTemplate, "server {\n    listen 80;", "\n}\n\nserver {\n    listen 443 ssl;");
		String httpsServer = defaultConfTemplate.substring(defaultConfTemplate.indexOf("server {\n    listen 443 ssl;"));

		assertTrue(defaultConfTemplate.contains("macro render_scanner_policy()"));
		assertEquals(2, countOccurrences(defaultConfTemplate, "{{ render_scanner_policy() }}"));
		assertBefore(httpServer, "location ^~ /.well-known/acme-challenge/", "{{ render_scanner_policy() }}");
		assertBefore(httpServer, "{{ render_scanner_policy() }}", "return 301 https://$host$request_uri;");
		assertBefore(httpsServer, "{{ render_scanner_policy() }}", "BEAT MANAGED GENERATED ROUTE INCLUDES");
		assertTrue(defaultConfTemplate.contains("location = {{ path }}"));
		assertTrue(defaultConfTemplate.contains("location ^~ {{ prefix }}"));
		assertTrue(defaultConfTemplate.contains("location ~ \"^/\\.env(?:\\.[A-Za-z0-9_-]{1,32})?$\""));
		assertFalse(defaultConfTemplate.contains("location ~ ^/\\.env"));
		assertFalse(defaultConfTemplate.contains(".*\\.php"));
		assertFalse(defaultConfTemplate.contains("limit_req "));
		assertFalse(defaultConfTemplate.contains("limit_req_zone"));

		assertTrue(defaults.contains("nginx_base_config_scanner_block_enabled: true"));
		assertTrue(defaults.contains("nginx_base_config_scanner_block_status: 404"));
		assertTrue(defaults.contains("- /.env"));
		assertTrue(defaults.contains("- /.git/config"));
		assertTrue(defaults.contains("- /wp-login.php"));
		assertTrue(defaults.contains("- /xmlrpc.php"));
		assertTrue(defaults.contains("- /index.php"));
		assertTrue(defaults.contains("- /phpinfo.php"));
		assertTrue(defaults.contains("- /info.php"));
		assertTrue(defaults.contains("- /wordpress/"));
		assertTrue(defaults.contains("- /wp-admin/"));
		assertTrue(defaults.contains("- /wp-content/"));
		assertTrue(defaults.contains("- /wp-includes/"));
		assertTrue(defaults.contains("- /laravel/"));
		assertTrue(defaults.contains("nginx_base_config_scanner_rate_limit_enabled: false"));
		assertTrue(defaults.contains("nginx_base_config_scanner_rate_limit_dry_run: true"));
		assertTrue(defaults.contains("nginx_base_config_scanner_rate_limit_status: 429"));

		assertTrue(tasks.contains("Validate nginx scanner block settings"));
		assertTrue(tasks.contains("nginx_base_config_scanner_block_status | int in [404, 444]"));
		assertTrue(tasks.contains("nginx_base_config_scanner_exact_paths is sequence"));
		assertTrue(tasks.contains("nginx_base_config_scanner_exact_paths is not string"));
		assertTrue(tasks.contains("nginx_base_config_scanner_exact_paths | select('string')"));
		assertTrue(tasks.contains("reject('match', '^/[A-Za-z0-9._/-]+$')"));
		assertTrue(tasks.contains("nginx_base_config_scanner_prefix_paths is sequence"));
		assertTrue(tasks.contains("nginx_base_config_scanner_prefix_paths is not string"));
		assertTrue(tasks.contains("nginx_base_config_scanner_prefix_paths | select('string')"));
		assertTrue(tasks.contains("reject('match', '^/[A-Za-z0-9._/-]+/$')"));

		assertTrue(infraReadme.contains("Scanner/bot nginx 차단 정책"));
		assertTrue(infraReadme.contains("기본 응답은 `404`"));
		assertTrue(infraReadme.contains("`444`는 운영 access log와 smoke 검증 후에만 선택"));
		assertTrue(infraReadme.contains("rate limit은 1차 rollout에서 강제 적용하지 않는다"));
		assertTrue(infraReadme.contains("app request completion log를 추가하지 않는다"));
	}

	@Test
	void observabilityOwnsSentryFullObservabilityContract() throws Exception {
		String versionCatalog = read("gradle/libs.versions.toml");
		String rootBuild = read("build.gradle.kts");
		String sentrySourceContextConvention = read("build-logic/src/main/kotlin/beat.sentry-source-context.gradle.kts");
		String prometheusRuntimeConvention = read("build-logic/src/main/kotlin/beat.prometheus-runtime.gradle.kts");
		String buildLogicBuild = read("build-logic/build.gradle.kts");
		String apisBuild = read("apis/build.gradle.kts");
		String adminBuild = read("admin/build.gradle.kts");
		String batchBuild = read("batch/build.gradle.kts");
		String observabilityBuild = read("observability/build.gradle.kts");
		String observabilityConfig = read("observability/src/main/kotlin/com/beat/observability/ObservabilityModuleConfig.kt");
		String sentryConfig = read("observability/src/main/kotlin/com/beat/observability/sentry/SentryConfig.kt");
		String sentryProcessor = read("observability/src/main/kotlin/com/beat/observability/sentry/BeatSentryEventProcessor.kt");
		String sentryMetrics = read("observability/src/main/kotlin/com/beat/observability/sentry/BeatSentryMetrics.kt");
		String sentrySensitivePolicy = read(
				"observability/src/main/kotlin/com/beat/observability/sentry/SentrySensitiveDataPolicy.kt");
		String observabilityYaml = read("observability/src/main/resources/application-observability.yml");
		String log4j2 = read("observability/src/main/resources/log4j2-spring.xml");
		String appContainerEnv = read("infra/ansible/roles/app_container_runtime/tasks/env.yml");
		String appRollback = read("infra/ansible/roles/app_rollback/tasks/main.yml");
		String appBluegreen = read("infra/ansible/roles/app_bluegreen/tasks/run_switch.yml");
		String appStopstart = read("infra/ansible/roles/app_stopstart/tasks/run_container.yml");
		String ciPr = read(".github/workflows/ci-pr.yml");
		String deployDev = read(".github/workflows/deploy-dev.yml");
		String deployProd = read(".github/workflows/deploy-prod.yml");
		String observabilityReadme = read("observability/README.md");
		String infraReadme = read("infra/README.md");

		assertTrue(versionCatalog.contains("sentry = \"8.41.0\""));
		assertTrue(versionCatalog.contains("sentry-gradle-plugin = \"6.6.0\""));
		assertTrue(versionCatalog.contains("io.sentry:sentry-spring-boot-4-starter"));
		assertTrue(versionCatalog.contains("io.sentry:sentry-async-profiler"));
		assertTrue(versionCatalog.contains("io.sentry:sentry-log4j2"));
		assertFalse(rootBuild.contains("includeSourceContext.set(true)"));
		assertFalse(rootBuild.contains("autoUploadSourceContext.set("));
		assertFalse(rootBuild.contains("authToken.set(providers.environmentVariable(\"SENTRY_AUTH_TOKEN\")"));
		assertFalse(rootBuild.contains("resolutionStrategy.force(\"io.sentry:sentry:$sentrySdkVersion\")"));
		assertTrue(buildLogicBuild.contains("sentry-gradle-plugin"));
		assertTrue(buildLogicBuild.contains("io.sentry.jvm.gradle.gradle.plugin"));
		assertTrue(sentrySourceContextConvention.contains("id(\"io.sentry.jvm.gradle\")"));
		assertTrue(sentrySourceContextConvention.contains("includeSourceContext.set(true)"));
		assertTrue(sentrySourceContextConvention.contains("autoUploadSourceContext.set("));
		assertTrue(sentrySourceContextConvention.contains("org.set(\"beat-jo\")"));
		assertTrue(sentrySourceContextConvention.contains("projectName.set(\"java-spring-boot\")"));
		assertTrue(sentrySourceContextConvention.contains("authToken.set(providers.environmentVariable(\"SENTRY_AUTH_TOKEN\")"));
		assertTrue(sentrySourceContextConvention.contains("autoInstallation {"));
		assertTrue(sentrySourceContextConvention.contains("enabled.set(false)"));
		assertTrue(sentrySourceContextConvention.contains("resolutionStrategy.force(\"io.sentry:sentry:$sentrySdkVersion\")"));
		assertTrue(sentrySourceContextConvention.contains("collectExternalDependenciesForSentry"));
		assertTrue(sentrySourceContextConvention.contains("generateSentry"));
		assertTrue(sentrySourceContextConvention.contains("explodeCodeSourceMain"));
		for (String moduleBuild : new String[] {
			"apis/build.gradle.kts",
			"admin/build.gradle.kts",
			"batch/build.gradle.kts",
			"observability/build.gradle.kts",
			"infra/build.gradle.kts",
			"domain/build.gradle.kts",
			"gateway/build.gradle.kts",
			"global-support/build.gradle.kts",
			"module-contracts/build.gradle.kts"
		}) {
			assertTrue(read(moduleBuild).contains("id(\"beat.sentry-source-context\")"), moduleBuild);
		}
		assertTrue(observabilityBuild.contains("libs.sentry.spring.boot.starter"));
		assertTrue(observabilityBuild.contains("libs.sentry.async.profiler"));
		assertTrue(observabilityBuild.contains("libs.sentry.log4j2"));
		assertTrue(prometheusRuntimeConvention.contains("add(\"runtimeOnly\", libs.findLibrary(\"micrometer-registry-prometheus\").get())"));
		assertTrue(apisBuild.contains("id(\"beat.prometheus-runtime\")"));
		assertTrue(batchBuild.contains("id(\"beat.prometheus-runtime\")"));
		assertTrue(adminBuild.contains("id(\"beat.prometheus-runtime\")"));
		assertFalse(apisBuild.contains("implementation(libs.micrometer.registry.prometheus)"));
		assertFalse(observabilityBuild.contains("libs.micrometer.registry.prometheus"));

		assertTrue(observabilityConfig.contains("SentryConfig::class"));
		assertTrue(sentryConfig.contains("options.isEnabled = false"));
		assertTrue(sentryConfig.contains("options.addEventProcessor(beatSentryEventProcessor)"));
		assertTrue(sentrySensitivePolicy.contains("authorization"));
		assertTrue(sentryProcessor.contains("BaseMdcLoggingFilter.TRACE_ID_KEY"));
		assertTrue(sentryProcessor.contains("BaseMdcLoggingFilter.ROUTE_PATTERN_KEY"));
		assertTrue(sentrySensitivePolicy.contains("cookie"));
		assertTrue(sentrySensitivePolicy.contains("SENTRY_AUTH_TOKEN")
				|| sentrySensitivePolicy.contains("sentry[-_]?auth[-_]?token"));
		assertTrue(sentryMetrics.contains("class BeatSentryMetrics"));
		assertTrue(sentryMetrics.contains("Sentry.metrics()"));
		assertTrue(sentryMetrics.contains("SentrySensitiveDataPolicy.isForbiddenMetricTag"));
		assertTrue(sentrySensitivePolicy.contains("forbiddenExactMetricTagKeys"));
		assertTrue(sentrySensitivePolicy.contains("forbiddenMetricTagFragments"));

		assertTrue(observabilityYaml.contains("dsn: ${SENTRY_DSN:}"));
		assertTrue(observabilityYaml.contains("sample-rate: 1.0"));
		assertTrue(observabilityYaml.contains("send-default-pii: true"));
		assertTrue(observabilityYaml.contains("enabled: true"));
		// Sentry distributed tracing and profiling are intentionally disabled (0.0).
		// Sentry is used for error event capture and Sentry Logs only.
		assertTrue(observabilityYaml.contains("traces-sample-rate: 0.0"));
		assertTrue(observabilityYaml.contains("profile-session-sample-rate: 0.0"));
		assertTrue(observabilityYaml.contains("profile-lifecycle: TRACE"));
		assertFalse(observabilityYaml.contains("DEV_SENTRY_DSN"));
		assertFalse(observabilityYaml.contains("PROD_SENTRY_DSN"));
		assertFalse(observabilityYaml.contains("PROD_SENTRY_TRACES_SAMPLE_RATE"));
		assertFalse(observabilityYaml.contains("PROD_SENTRY_PROFILE_SESSION_SAMPLE_RATE"));
		assertFalse(observabilityYaml.contains("enable-tracing"));
		assertTrue(log4j2.contains("<Sentry name=\"SentryAppender\""));
		assertTrue(log4j2.contains("<AppenderRef ref=\"SentryAppender\"/>"));

		assertTrue(appContainerEnv.contains("'SENTRY_RELEASE': 'beat-server@' ~ ("));
		assertTrue(appContainerEnv.contains("app_container_runtime_release_ref"));
		assertTrue(appContainerEnv.contains("default(commit_sha | default(image_tag | default('unknown', true), true), true)"));
		assertTrue(appRollback.contains("app_rollback_previous_release.commit_sha"));
		assertTrue(appRollback.contains("app_rollback_previous_release.image_tag"));
		assertTrue(appBluegreen.contains("app_container_runtime_release_ref: \"{{ app_bluegreen_release_ref | default('', true) }}\""));
		assertTrue(appStopstart.contains("app_container_runtime_release_ref: \"{{ app_stopstart_release_ref | default('', true) }}\""));
		assertTrue(ciPr.contains("SENTRY_AUTH_TOKEN: ${{ secrets.SENTRY_AUTH_TOKEN }}"));
		assertTrue(ciPr.contains("SENTRY_RELEASE: beat-server@${{ github.sha }}"));
		// deploy-dev resolves the deploy ref via `resolve-ref` step, so SENTRY_RELEASE pins to the resolved commit.
		assertTrue(deployDev.contains("SENTRY_RELEASE: beat-server@${{ needs.resolve-ref.outputs.commit_sha }}"));
		assertTrue(deployProd.contains("SENTRY_RELEASE: beat-server@${{ needs.resolve-release.outputs.commit_sha }}"));
		assertFalse(appContainerEnv.contains("SENTRY_AUTH_TOKEN"));
		assertTrue(observabilityReadme.contains("Sentry는 `observability` 모듈이 소유"));
		assertTrue(observabilityReadme.contains("app request completion log는 추가하지 않습니다"));
		assertTrue(infraReadme.contains("SENTRY_DSN=https://public@example.ingest.sentry.io/project-id"));
		assertFalse(infraReadme.contains("DEV_SENTRY_DSN="));
		assertFalse(infraReadme.contains("PROD_SENTRY_DSN="));
		assertTrue(infraReadme.contains("SENTRY_AUTH_TOKEN=<Sentry organization token"));
	}

	@Test
	void batchRemainsTheSchedulerOwnerLaneAfterRootRetirement() throws Exception {
		Path schedulerService = Path.of(
			"batch/src/main/java/com/beat/batch/scheduler/application/JobSchedulerService.java");
		Path schedulerTransactionalService = Path.of(
			"batch/src/main/java/com/beat/batch/scheduler/application/JobSchedulerTransactionalService.java");
		Path ticketCleanupService = Path.of(
			"batch/src/main/java/com/beat/batch/booking/application/TicketCleanupService.java");
		Path promotionMaintenanceService = Path.of(
			"batch/src/main/java/com/beat/batch/promotion/application/PromotionMaintenanceService.java");

		assertTrue(Files.exists(schedulerService));
		assertTrue(Files.exists(schedulerTransactionalService));
		assertTrue(Files.exists(ticketCleanupService));
		assertTrue(Files.exists(promotionMaintenanceService));
		assertTrue(Files.readString(schedulerService).startsWith("package com.beat.batch.scheduler.application;"));
		assertTrue(Files.readString(schedulerTransactionalService)
			.startsWith("package com.beat.batch.scheduler.application;"));
		assertTrue(Files.readString(ticketCleanupService).startsWith("package com.beat.batch.booking.application;"));
		assertTrue(
			Files.readString(promotionMaintenanceService).startsWith("package com.beat.batch.promotion.application;"));
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
		assertTrue(ciPr.contains("./gradlew check verifyModuleBootJars --parallel --build-cache"));
		assertFalse(ciPr.contains("verifyV2WebBaseline"));
		assertTrue(deployDev.contains("./gradlew check verifyModuleBootJars --parallel --build-cache"));
		assertFalse(deployDev.contains("verifyV2WebBaseline"));
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
		assertTrue(deployProd.contains("secret-preflight:"));
		assertTrue(deployProd.contains("Resolve prod SSH connection metadata"));
		assertTrue(deployProd.contains("Verify prod encrypted inventory, resolver, and lint"));
		assertTrue(deployProd.contains("Prod secret-aware preflight verified resolver for module=${MODULE}"));
		assertTrue(deployProd.contains("ansible-lint playbooks/*.yml roles"));
		assertTrue(deployProd.contains("git merge-base --is-ancestor \"$COMMIT_SHA\" refs/remotes/origin/main"));
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
		assertTrue(deployProd.contains("^v[0-9]+\\.[0-9]+\\.[0-9]+$"));
		assertTrue(deployProd.contains("Invalid release tag: $RELEASE_TAG (expected vX.Y.Z)"));
		assertTrue(deployProd.contains("module_matrix"));
		assertTrue(deployProd.contains("matrix.module"));
		assertTrue(deployProd.contains("commit_sha"));
		assertTrue(deployProd.contains("ref: ${{ needs.resolve-release.outputs.commit_sha }}"));
		assertTrue(deployProd.contains("checkout_ref: ${{ needs.resolve-release.outputs.commit_sha }}"));
		assertTrue(deployProd.contains("- secret-preflight"));
		assertTrue(deployProd.contains("Resolve prod SSH connection metadata"));
		assertTrue(deployProd.contains("Prod secret-aware preflight verified resolver for module=${MODULE}"));
		assertTrue(deployProd.contains("ansible-lint playbooks/*.yml roles"));
		assertTrue(deployProd.contains("git merge-base --is-ancestor \"$COMMIT_SHA\" refs/remotes/origin/main"));
		assertFalse(deployProd.contains("workflow_dispatch:"));
		assertFalse(deployProd.contains("github.event.inputs.version"));
		assertFalse(deployProd.contains("github.event.inputs.module"));
		assertFalse(deployProd.contains("checkout_ref=refs/tags/"));
		String prodModuleMatrix =
			"module_matrix={\"include\":[{\"module\":\"admin\"},{\"module\":\"apis\"},{\"module\":\"batch\"}]}";
		assertTrue(deployProd.contains(prodModuleMatrix));

		assertTrue(secretAwareVerify.contains("module: ${{ matrix.module }}"));
		assertTrue(secretAwareVerify.contains("Verified resolver for module=${MODULE}"));
		assertTrue(secretAwareVerify.contains("- admin"));
		assertTrue(secretAwareVerify.contains("- batch"));
	}

	@Test
	void foundationMarkerContractProtectsDeployAndRollback() throws Exception {
		String ansibleExecWorkflow = read(".github/workflows/_ansible-exec.yml");
		String deployDev = read(".github/workflows/deploy-dev.yml");
		String deployProd = read(".github/workflows/deploy-prod.yml");
		String foundationPlaybook = read("infra/ansible/playbooks/foundation.yml");
		String deployPlaybook = read("infra/ansible/playbooks/deploy.yml");
		String rollbackPlaybook = read("infra/ansible/playbooks/rollback.yml");
		String nginxFragmentsPreflight = read("infra/ansible/playbooks/tasks/validate_nginx_fragments.yml");
		String infraReadme = read("infra/README.md");

		assertTrue(ansibleExecWorkflow.contains("connection_module:"));
		assertTrue(ansibleExecWorkflow.contains(
			"module: ${{ inputs.connection_module != '' && inputs.connection_module || inputs.module }}"));

		assertTrue(foundationPlaybook.contains("foundation_marker_path: \"{{ deployment_dir }}/.foundation-applied\""));
		assertTrue(foundationPlaybook.contains("tasks/validate_nginx_fragments.yml"));
		assertTrue(foundationPlaybook.contains("name: Assert required foundation marker inputs"));
		assertTrue(foundationPlaybook.contains("deploy_environment is defined"));
		assertTrue(foundationPlaybook.contains("post_tasks:"));
		assertTrue(foundationPlaybook.contains("name: Mark foundation as applied"));
		assertTrue(foundationPlaybook.contains("applied_at: {{ now(utc=true, fmt='%Y-%m-%dT%H:%M:%SZ') }}"));
		assertTrue(foundationPlaybook.contains("commit_sha: {{ commit_sha | default('unknown') }}"));
		assertTrue(foundationPlaybook.contains("deploy_environment: {{ deploy_environment }}"));
		assertTrue(foundationPlaybook.contains("foundation_mysql_enabled: {{ foundation_mysql_enabled | default(true) }}"));
		assertTrue(foundationPlaybook.contains("foundation_redis_enabled: {{ foundation_redis_enabled | default(true) }}"));
		assertTrue(foundationPlaybook.contains("foundation_manage_nginx: {{ foundation_manage_nginx | default(false) }}"));
		assertBefore(foundationPlaybook, "role: foundation_stack", "name: Mark foundation as applied");
		assertBefore(foundationPlaybook, "role: nginx_base_config", "name: Mark foundation as applied");

		assertTrue(deployPlaybook.contains("foundation_marker_path: \"{{ deployment_dir }}/.foundation-applied\""));
		assertTrue(deployPlaybook.contains("tasks/validate_nginx_fragments.yml"));
		assertTrue(deployPlaybook.contains("name: Stat foundation marker"));
		assertTrue(deployPlaybook.contains("register: deploy_foundation_marker_stat"));
		assertTrue(deployPlaybook.contains("name: Read foundation marker for diagnostics"));
		assertTrue(deployPlaybook.contains("register: deploy_foundation_marker_raw"));
		assertTrue(deployPlaybook.contains("name: Abort deploy when foundation is not applied"));
		assertTrue(deployPlaybook.contains("Foundation has not been applied on host {{ inventory_hostname }}."));
		assertTrue(deployPlaybook.contains("inventories/<env>/hosts.yml"));
		assertFalse(deployPlaybook.contains("default(['inventories/dev/hosts.yml'])"));
		assertTrue(deployPlaybook.contains("Or trigger the foundation step on GitHub Actions before retrying deploy."));
		assertTrue(deployPlaybook.contains("name: Report foundation marker contents"));
		assertBefore(deployPlaybook, "name: Stat foundation marker", "role: app_secret");

		assertTrue(rollbackPlaybook.contains("foundation_marker_path: \"{{ deployment_dir }}/.foundation-applied\""));
		assertTrue(rollbackPlaybook.contains("tasks/validate_nginx_fragments.yml"));
		assertTrue(rollbackPlaybook.contains("name: Stat foundation marker"));
		assertTrue(rollbackPlaybook.contains("register: rollback_foundation_marker_stat"));
		assertTrue(rollbackPlaybook.contains("name: Read foundation marker for diagnostics"));
		assertTrue(rollbackPlaybook.contains("register: rollback_foundation_marker_raw"));
		assertTrue(rollbackPlaybook.contains("name: Abort rollback when foundation is not applied"));
		assertTrue(rollbackPlaybook.contains("inventories/<env>/hosts.yml"));
		assertFalse(rollbackPlaybook.contains("default(['inventories/dev/hosts.yml'])"));
		assertTrue(rollbackPlaybook.contains("Or trigger the foundation step on GitHub Actions before retrying rollback."));
		assertTrue(rollbackPlaybook.contains("name: Report foundation marker contents"));
		assertBefore(rollbackPlaybook, "name: Stat foundation marker", "name: Roll back runtime to previous release");

		assertBefore(deployDev, "\n  foundation:", "\n  deploy:");
		String devFoundationNeeds =
			"  foundation:\n"
				+ "    needs:\n"
				+ "      - detect-changes\n"
				+ "      - resolve-ref\n"
				+ "      - verify\n"
				+ "      - build-image";
		String devDeployNeedsFoundation =
			"      - build-image\n"
				+ "      - foundation\n"
				+ "    if: needs.detect-changes.outputs.has_modules == 'true'";
		assertTrue(deployDev.contains(devFoundationNeeds));
		assertTrue(deployDev.contains("module: foundation"));
		assertTrue(deployDev.contains("connection_module: ${{ vars.DEV_FOUNDATION_CONNECTION_MODULE || 'apis' }}"));
		assertTrue(deployDev.contains("playbook: playbooks/foundation.yml"));
		// commit_sha / checkout_ref now pin to the resolved deploy ref (supports manual deploy_ref input).
		assertTrue(deployDev.contains("commit_sha: ${{ needs.resolve-ref.outputs.commit_sha }}"));
		assertTrue(deployDev.contains("checkout_ref: ${{ needs.resolve-ref.outputs.commit_sha }}"));
		assertTrue(deployDev.contains(devDeployNeedsFoundation));
		// dev-runtime concurrency group serializes deploys to the single dev runtime cluster.
		assertTrue(deployDev.contains("group: dev-runtime"));

		assertBefore(deployProd, "\n  foundation:", "\n  deploy:");
		String prodFoundationNeeds =
			"  foundation:\n"
				+ "    needs:\n"
				+ "      - resolve-release\n"
				+ "      - verify\n"
				+ "      - secret-preflight\n"
				+ "      - build-image";
		String prodDeployNeedsFoundation =
			"      - secret-preflight\n"
				+ "      - build-image\n"
				+ "      - foundation\n"
				+ "    concurrency:\n"
				+ "      group: prod-runtime";
		assertTrue(deployProd.contains(prodFoundationNeeds));
		assertTrue(deployProd.contains("module: foundation"));
		assertTrue(deployProd.contains("connection_module: ${{ vars.PROD_FOUNDATION_CONNECTION_MODULE || 'apis' }}"));
		assertTrue(deployProd.contains("playbook: playbooks/foundation.yml"));
		assertTrue(deployProd.contains("commit_sha: ${{ needs.resolve-release.outputs.commit_sha }}"));
		assertTrue(deployProd.contains("checkout_ref: ${{ needs.resolve-release.outputs.commit_sha }}"));
		assertTrue(deployProd.contains(prodDeployNeedsFoundation));
		assertTrue(deployProd.contains("group: prod-runtime"));
		assertTrue(infraReadme.contains(
			"`deploy-prod.yml`의 `resolve-release` → `verify` + `secret-preflight` → `build-image` → `foundation` → `deploy` 순서를 확인한다."));

		assertTrue(infraReadme.contains("Foundation marker contract"));
		assertTrue(infraReadme.contains("{{ deployment_dir }}/.foundation-applied"));
		assertTrue(infraReadme.contains("applied_at`, `commit_sha`, `deploy_environment`"));
		assertTrue(infraReadme.contains("DEV_FOUNDATION_CONNECTION_MODULE"));
		assertTrue(infraReadme.contains("PROD_FOUNDATION_CONNECTION_MODULE"));
		assertTrue(nginxFragmentsPreflight.contains("nginx_fragments is mapping"));
		assertTrue(nginxFragmentsPreflight.contains("nginx_fragments mapping has invalid or duplicate entries"));
		assertTrue(nginxFragmentsPreflight.contains("nginx_fragment_files | unique | list | length"));
		assertTrue(nginxFragmentsPreflight.contains("modules is mapping"));
		assertTrue(nginxFragmentsPreflight.contains("((modules | default({})).apis | default({})).backend_upstream_name"));
		assertTrue(nginxFragmentsPreflight.contains(
			"(((modules | default({})).admin | default({})).nginx_route | default({})).upstream_name"));
	}

	@Test
	void deploymentInfraUsesRepoOwnedHelpersAndConfiguredModuleContracts() throws Exception {
		String dockerfileModule = read("Dockerfile.module");
		String dockerignore = read(".dockerignore");
		String nginxUpdateScript = read("infra/ansible/roles/nginx_config_helper/files/update-nginx-config.py");
		String foundationPlaybook = read("infra/ansible/playbooks/foundation.yml");
		String foundationStackTasks = read("infra/ansible/roles/foundation_stack/tasks/main.yml");
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
		String adminNginxRoute = read("infra/ansible/roles/app_stopstart/tasks/admin_nginx_route.yml");
		String deployDev = read(".github/workflows/deploy-dev.yml");
		String deployProd = read(".github/workflows/deploy-prod.yml");
		String rollbackProd = read(".github/workflows/rollback-prod.yml");
		String devInventory = read("infra/ansible/inventories/dev/group_vars/all/main.yml");
		String prodInventory = read("infra/ansible/inventories/prod/group_vars/all/main.yml");

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
		assertFalse(Files.exists(Path.of("infra/ansible/roles/nginx_config_helper/tasks/migrate_legacy_upstreams.yml")));
		assertTrue(Files.exists(Path.of("scripts/generate-local-dev-secret.sh")));
		assertTrue(Files.exists(Path.of("scripts/generate-local-prod-secret.sh")));
		assertTrue(Files.exists(Path.of(".dockerignore")));
		assertTrue(dockerfileModule.contains("ARG MODULE"));
		// JAR is built outside the container (ARM native build) and COPY'd in — no in-container build stage.
		assertTrue(dockerfileModule.contains("COPY --chown=beat:beat app.jar /app/app.jar"));
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
		assertTrue(appContainerRuntimeEnv.contains("'SPRING_PROFILES_ACTIVE': app_container_runtime_module_cfg.spring_profile"));
		assertFalse(appBluegreenRunSwitch.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\""));
		assertFalse(appBluegreenRunSwitch.contains("SPRING_PROFILES_ACTIVE"));
		assertTrue(appStopStartRole.contains("run_container.yml"));
		assertTrue(appStopStartRunContainer.contains("community.docker.docker_container"));
		assertTrue(appContainerRuntimeEnv.contains("BEAT_SCHEDULER_OWNER"));
		assertTrue(appStopStartRunContainer.contains("app_container_env"));
		assertTrue(appStopStartRunContainer.contains("name: app_container_runtime"));
		assertTrue(appStopStartRunContainer.contains(
			"healthcheck_target_container: \"{{ app_stopstart_module_cfg.container_name }}\""));
		assertFalse(appStopStartRunContainer.contains("| default(module)"));
		assertTrue(appContainerRuntimeEnv.contains("| combine({"));
		assertTrue(appContainerRuntimeEnv.contains("| string | lower"));
		assertFalse(appStopStartRunContainer.contains("\"{{ module_cfg.spring_profile | upper }}_ACTUATOR_PORT\""));
		assertFalse(appStopStartRunContainer.contains("SPRING_PROFILES_ACTIVE"));
		assertFalse(appStopStartRunContainer.contains("\n    ports:"));
		assertFalse(appStopStartRunContainer.contains("0.0.0.0:400"));
		assertFalse(appBluegreenRunSwitch.contains("\n        ports:"));
		assertFalse(appBluegreenRunSwitch.contains("0.0.0.0:400"));
		assertTrue(foundationPlaybook.contains("role: foundation_stack"));
		assertTrue(foundationPlaybook.contains("role: nginx_base_config"));
		assertTrue(foundationStackTasks.contains("project_src: \"{{ deployment_dir }}\""));
		assertTrue(foundationStackTasks.contains("- docker-compose.yml"));
		assertTrue(foundationStackTasks.contains("Ensure nginx bind mount and candidate directories exist"));
		assertTrue(foundationStackTasks.contains("Migrate legacy nginx named volume config to bind mount"));
		assertTrue(foundationStackTasks.contains("Migrate legacy nginx named volume fragments to bind mount"));
		assertTrue(foundationStackTasks.contains("- -anv"));
		assertTrue(foundationStackTasks.contains("foundation_stack_legacy_config_migration_result.stdout"));
		assertTrue(foundationStackTasks.contains("Inspect legacy nginx named volume metadata"));
		assertTrue(foundationStackTasks.contains("community.docker.docker_volume_info"));
		assertTrue(foundationStackTasks.contains("foundation_stack_legacy_nginx_volume_info.exists"));
		assertTrue(foundationStackTasks.contains("when: not foundation_stack_bind_migration_marker_stat.stat.exists"));
		assertFalse(foundationStackTasks.contains("- docker\n      - volume\n      - inspect"));
		assertTrue(foundationStackTasks.contains(".bind-mount-migrated-from-{{ foundation_stack_legacy_nginx_volume_name"));
		assertTrue(foundationStackTasks.contains("Remove stale nginx helper lock files from deployment-owned nginx tree"));
		assertFalse(foundationStackTasks.contains("/var/lib/docker/volumes"));
		assertFalse(foundationStackTasks.contains("foundation_stack_compose_definition"));
		assertFalse(foundationStackTasks.contains("definition: \"{{ foundation_stack_compose_definition }}\""));
		assertTrue(foundationComposeTemplate.contains("services:"));
		assertTrue(foundationComposeTemplate.contains("container_name: \"{{ nginx_container_name }}\""));
		assertTrue(foundationComposeTemplate.contains("- \"80:80\""));
		assertTrue(foundationComposeTemplate.contains("- \"443:443\""));
		assertTrue(foundationComposeTemplate.contains("{{ deployment_dir }}/nginx/conf.d:/etc/nginx/conf.d"));
		assertTrue(foundationComposeTemplate.contains("{{ deployment_dir }}/nginx/generated:/etc/nginx/generated"));
		assertFalse(foundationComposeTemplate.contains(":/etc/nginx\""));
		assertFalse(foundationComposeTemplate.contains("nginx-config-volume"));
		assertTrue(foundationComposeTemplate.contains("foundation_mysql_enabled"));
		assertTrue(foundationComposeTemplate.contains("- \"127.0.0.1:3306:3306\""));
		assertTrue(foundationComposeTemplate.contains("foundation_redis_enabled"));
		assertFalse(defaultConfTemplate.contains("upstream {{ backend_upstream_name"));
		assertFalse(defaultConfTemplate.contains("upstream {{ actuator_upstream_name"));
		assertTrue(defaultConfTemplate.contains("location {{ actuator_path }}/"));
		assertFalse(defaultConfTemplate.contains("location /admin/"));
		assertTrue(defaultConfTemplate.contains("BEAT MANAGED GENERATED UPSTREAM INCLUDES"));
		assertTrue(defaultConfTemplate.contains("BEAT MANAGED GENERATED ROUTE INCLUDES"));
		assertTrue(defaultConfTemplate.contains("escape=json"));
		assertTrue(defaultConfTemplate.contains("\"trace_id\":\"$request_id\""));
		assertTrue(defaultConfTemplate.contains("\"client_ip\":\"$remote_addr\""));
		assertTrue(defaultConfTemplate.contains("\"request\":\"$request\""));
		assertTrue(defaultConfTemplate.contains("\"status\":\"$status\""));
		assertTrue(defaultConfTemplate.contains("\"bytes\":$body_bytes_sent"));
		assertTrue(defaultConfTemplate.contains("\"referer\":\"$http_referer\""));
		assertTrue(defaultConfTemplate.contains("\"user_agent\":\"$http_user_agent\""));
		assertTrue(defaultConfTemplate.contains("\"x_forwarded_for\":\"$http_x_forwarded_for\""));
		assertTrue(defaultConfTemplate.contains("\"request_time\":$request_time"));
		assertEquals(
			2,
			countOccurrences(defaultConfTemplate, "access_log /var/log/nginx/access.log {{ nginx_access_log_format_name }}"));
		assertTrue(defaultConfTemplate.contains("proxy_set_header X-Request-ID $request_id"));
		assertTrue(infraReadme.contains("HTTP request completion logging은 nginx `access.log`가 소유한다"));
		assertTrue(infraReadme.contains("application log는 business/domain event 중심"));
		assertTrue(infraReadme.contains("app container(`apis`, `admin`, `batch`) run task에는 `ports:`를 추가하지 않는다"));
		assertTrue(nginxUpdateScript.contains("BEAT MANAGED GENERATED UPSTREAM INCLUDES"));
		assertTrue(nginxUpdateScript.contains("BEAT MANAGED GENERATED ROUTE INCLUDES"));
		assertTrue(nginxUpdateScript.contains("bootstrap-includes"));
		assertTrue(nginxUpdateScript.contains("upsert-upstream"));
		assertFalse(nginxUpdateScript.contains("split-upstreams"));
		assertFalse(nginxUpdateScript.contains("split_upstreams"));
		assertFalse(nginxUpdateScript.contains("skip_existing"));
		assertTrue(nginxUpdateScript.contains("json.dumps({\"changed\": changed})"));
		assertTrue(nginxUpdateScript.contains("LOCK_DIR_ENV = \"BEAT_NGINX_LOCK_DIR\""));
		assertTrue(nginxUpdateScript.contains("import hashlib"));
		assertTrue(nginxUpdateScript.contains("DEFAULT_LOCK_DIR = Path(\"/run/lock/beat-nginx\")"));
		assertTrue(nginxUpdateScript.contains("lock_root.mkdir(parents=True, exist_ok=True)"));
		assertTrue(nginxUpdateScript.contains("Path(tempfile.gettempdir()) / \"beat-nginx-locks\""));
		assertTrue(nginxUpdateScript.contains("def lock_filename(path: Path) -> str:"));
		assertFalse(nginxUpdateScript.contains("lock_path = path.parent / (path.name + \".lock\")"));
		assertFalse(deployPlaybook.contains("app_dev_switch"));
		assertFalse(deployPlaybook.contains("app_prod_switch"));
		assertTrue(deployPlaybook.contains("name: app_bluegreen"));
		assertTrue(deployPlaybook.contains("tasks_from: run_switch.yml"));
		assertTrue(deployPlaybook.contains("modules[module].deploy_mode == \"blue_green\""));
		assertTrue(deployPlaybook.contains("modules[module].deploy_mode == \"stop_start\""));
		String postFailureRestoreValidationSuccess =
			"(app_bluegreen_post_failure_restore_validate_result.rc | default(1)) == 0";
		assertTrue(appBluegreenRunSwitch.contains(postFailureRestoreValidationSuccess));
		assertTrue(deployPlaybook.contains("tags:"));
		assertTrue(deployPlaybook.contains("- healthcheck"));
		assertTrue(deployPlaybook.contains("- cleanup"));
		assertTrue(rollbackPlaybook.contains("name: app_healthcheck"));
		assertTrue(rollbackPlaybook.contains("module in modules"));
		assertTrue(rollbackPlaybook.contains("name: Roll back runtime to previous release"));
		assertTrue(rollbackPlaybook.contains("name: Execute rollback and restore metadata truth"));
		assertTrue(rollbackPlaybook.contains("app_rollback_module_cfg.nginx_route is defined"));
		assertTrue(rollbackPlaybook.contains("name: Restore stop-start current release after rollback failure"));
		assertTrue(rollbackPlaybook.contains("app_stopstart_image: \"{{ app_rollback_current_release.image }}\""));
		assertTrue(rollbackPlaybook.contains("name: Healthcheck restored stop-start current release"));
		assertTrue(rollbackPlaybook.contains("app_rollback_module_cfg.deploy_mode == 'stop_start'"));
		assertTrue(rollbackPlaybook.contains("For stop-start modules, the playbook attempted to restore the archived current image"));
		assertBefore(rollbackPlaybook, "name: Roll back runtime to previous release", "name: Healthcheck module after rollback before metadata promotion");
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
		// rollback-prod.yml delegates ref validation to the playbook itself; no in-workflow merge-base guard.
		assertTrue(rollbackProd.contains("module: ${{ github.event.inputs.module }}"));
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
		assertTrue(deployDev.contains("IMAGE_TAG=\"dev-${RESOLVED_SHA}\""));
		String devRuntimeImage =
			"image: ${{ vars.DEV_DOCKER_LOGIN_USERNAME }}/beat-${{ matrix.module }}:dev-${{ needs.resolve-ref.outputs.commit_sha }}";
		String prodRuntimeImage =
			"image: ${{ vars.PROD_DOCKER_LOGIN_USERNAME }}/beat-${{ matrix.module }}:"
				+ "${{ needs.resolve-release.outputs.release_tag }}";
		assertTrue(deployDev.contains(devRuntimeImage));
		assertTrue(deployProd.contains("IMAGE_TAG=\"${RELEASE_TAG}\""));
		assertTrue(deployProd.contains(prodRuntimeImage));
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
		assertTrue(appHealthcheckProbe.contains("printf 'health attempt %s/%s failed"));
		assertTrue(appHealthcheckProbe.contains("if [ \"$attempt\" -lt \"$attempts\" ]; then"));
		assertFalse(appHealthcheckRole.contains("current-slot"));
		assertFalse(appHealthcheckRole.contains("slurp:"));
		assertFalse(appHealthcheckRole.contains("module_cfg.container_name | default(module)"));
		assertFalse(appHealthcheckRole.contains("target=\"apis-$slot\""));
		assertTrue(appCleanupRole.contains("docker_image_prune_retention | default('72h')"));
		assertTrue(appCleanupRole.contains("docker_image_prune_failure_policy | default('warn') in ['warn', 'fail']"));
		assertTrue(appCleanupRole.contains("docker_image_prune_failure_policy | default('warn') == 'fail'"));
		assertTrue(appCleanupRole.contains("Total reclaimed space:\\\\s*0\\\\s*B"));
		assertTrue(appCleanupRole.contains("app_cleanup_docker_prune_result.stdout | default('unknown error', true)"));
		assertFalse(appCleanupRole.contains("until=72h"));
		assertFalse(appCleanupRole.contains("failed_when: false"));
		assertTrue(appRollbackRole.contains("app_rollback_archive_timestamp"));
		assertTrue(appRollbackRole.contains("now(utc=true, fmt='%Y%m%dT%H%M%SZ')"));
		assertTrue(appRollbackRole.contains("name: Read current release metadata before rollback"));
		assertTrue(appRollbackRole.contains("app_rollback_current_release_raw"));
		assertTrue(appRollbackRole.contains("app_rollback_current_release"));
		assertBefore(appRollbackRole, "name: Parse current release metadata before rollback", "name: Roll back stop-start lanes via Docker module");
		assertFalse(appRollbackRole.contains("lookup('pipe', 'date -u"));
		assertTrue(infraReadme.contains("Release metadata schema"));
		assertTrue(infraReadme.contains("created_at`은 원격 EC2의 시스템 시간이 아니라 controller UTC"));
		assertTrue(infraReadme.contains("SSH pipelining + sudo `requiretty` caveat"));
		assertTrue(infraReadme.contains("Defaults requiretty"));
		assertTrue(infraReadme.contains("Seed placeholder upstreams"));
		assertTrue(infraReadme.contains("nginx_seed_placeholder_host:nginx_seed_placeholder_port"));
		assertTrue(infraReadme.contains("127.0.0.1:65535"));
		assertTrue(infraReadme.contains("community.docker.docker_volume_info"));
		assertTrue(infraReadme.contains("nginx_legacy_config_volume_name"));
		assertFalse(infraReadme.contains("/var/lib/docker/volumes"));
		assertTrue(infraReadme.contains("Nginx fragment mapping contract"));
		assertTrue(infraReadme.contains("nginx_fragments"));
		assertTrue(infraReadme.contains("read-only contract"));
		assertTrue(infraReadme.contains("Prod rollback rehearsal 절차"));
		assertTrue(infraReadme.contains("legacyv1"));
		assertTrue(infraReadme.contains("Restore stop-start current release after rollback failure"));
		assertTrue(devInventory.contains("nginx_seed_placeholder_host: \"127.0.0.1\""));
		assertTrue(devInventory.contains("nginx_seed_placeholder_port: 65535"));
		assertTrue(devInventory.contains("nginx_legacy_config_volume_name: nginx-config-volume"));
		assertFalse(devInventory.contains("nginx_config_volume_name:"));
		assertTrue(devInventory.contains("nginx_conf_target_path: /home/ubuntu/deployment/nginx/conf.d/default.conf"));
		assertTrue(devInventory.contains("nginx_generated_source_dir: /home/ubuntu/deployment/nginx/generated-source"));
		assertTrue(devInventory.contains("nginx_generated_target_dir: /home/ubuntu/deployment/nginx/generated"));
		assertFalse(devInventory.contains("/var/lib/docker/volumes/nginx-config-volume/_data"));
		assertTrue(devInventory.contains("nginx_fragments:"));
		assertTrue(devInventory.contains("fragment_file: backend.conf"));
		assertTrue(devInventory.contains("fragment_file: admin_backend.conf"));
		assertTrue(devInventory.contains("fragment_file: actuator.conf"));
		assertTrue(devInventory.contains("fragment_file: 10-managed.conf"));
		assertTrue(prodInventory.contains("nginx_seed_placeholder_host: \"127.0.0.1\""));
		assertTrue(prodInventory.contains("nginx_seed_placeholder_port: 65535"));
		assertTrue(prodInventory.contains("nginx_legacy_config_volume_name: nginx-config-volume"));
		assertFalse(prodInventory.contains("nginx_config_volume_name:"));
		assertTrue(prodInventory.contains("nginx_conf_target_path: /home/ubuntu/deployment/nginx/conf.d/default.conf"));
		assertTrue(prodInventory.contains("nginx_generated_source_dir: /home/ubuntu/deployment/nginx/generated-source"));
		assertTrue(prodInventory.contains("nginx_generated_target_dir: /home/ubuntu/deployment/nginx/generated"));
		assertFalse(prodInventory.contains("/var/lib/docker/volumes/nginx-config-volume/_data"));
		assertTrue(prodInventory.contains("nginx_fragments:"));
		assertTrue(nginxBaseConfig.contains("nginx_base_config_transaction_operations"));
		assertTrue(nginxBaseConfig.contains("sync-backend-upstream-target"));
		assertFalse(nginxBaseConfig.contains("nginx_base_config_upstream_target_sync_result is defined"));
		assertTrue(nginxBaseConfig.contains("nginx_seed_placeholder_host"));
		assertTrue(nginxBaseConfig.contains("nginx_seed_placeholder_port"));
		assertTrue(nginxBaseConfig.contains("nginx_fragments.backend.fragment_file"));
		assertTrue(nginxBaseConfig.contains("nginx_fragments.admin.fragment_file"));
		assertTrue(nginxBaseConfig.contains("nginx_fragments.actuator.fragment_file"));
		assertTrue(nginxBaseConfig.contains("nginx_fragments.route.fragment_file"));
		assertTrue(nginxBaseConfig.contains("nginx_fragments.backend.upstream_name"));
		assertTrue(nginxBaseConfig.contains("nginx_fragments.admin.upstream_name"));
		assertTrue(nginxBaseConfig.contains("nginx_fragments.actuator.upstream_name"));
		assertFalse(nginxBaseConfig.contains("- \"127.0.0.1\"\n"
			+ "            - --backend-port\n"
			+ "            - \"65535\""));
		assertFalse(nginxBaseConfig.contains("/upstreams/backend.conf\""));
		assertFalse(nginxBaseConfig.contains("/upstreams/admin_backend.conf\""));
		assertFalse(nginxBaseConfig.contains("/upstreams/actuator.conf\""));
		assertFalse(nginxBaseConfig.contains("/routes/10-managed.conf\""));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragments.backend.fragment_file"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragments.actuator.fragment_file"));
		assertFalse(appBluegreenRunSwitch.contains("/upstreams/backend.conf\""));
		assertFalse(appBluegreenRunSwitch.contains("/upstreams/actuator.conf\""));
		assertBefore(
			appBluegreenRunSwitch,
			"nginx_fragment_transaction_operations:",
			"nginx_fragment_transaction_failure_summary:");
		assertBefore(
			adminNginxRoute,
			"app_stopstart_admin_nginx_transaction_operations:",
			"nginx_fragment_transaction_failure_summary:");
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_files:"));
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_operations:"));
		assertTrue(adminNginxRoute.contains("name: nginx_fragment_transaction"));
		assertTrue(adminNginxRoute.contains("nginx_fragment_transaction_id: app-stopstart-admin-route"));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_files: \"{{ app_stopstart_admin_nginx_transaction_files }}\""));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_operations: \"{{ app_stopstart_admin_nginx_transaction_operations }}\""));
		assertFalse(adminNginxRoute.contains("nginx_fragment_transaction_validate_command:"));
		assertFalse(adminNginxRoute.contains("nginx_fragment_transaction_reload_command:"));
		assertTrue(adminNginxRoute.contains("bootstrap-includes"));
		assertFalse(adminNginxRoute.contains("split-upstreams"));
		assertFalse(adminNginxRoute.contains("split_upstreams"));
		assertTrue(adminNginxRoute.contains("upsert-upstream"));
		assertTrue(adminNginxRoute.contains("ensure-route"));
		assertTrue(adminNginxRoute.contains("sync-admin-upstream-target"));
		assertTrue(adminNginxRoute.contains("sync-admin-route-target"));
		assertTrue(adminNginxRoute.contains("nginx_fragments.admin.fragment_file"));
		assertTrue(adminNginxRoute.contains("nginx_fragments.route.fragment_file"));
		assertFalse(adminNginxRoute.contains("/upstreams/admin_backend.conf\""));
		assertFalse(adminNginxRoute.contains("/routes/10-managed.conf\""));
		assertFalse(adminNginxRoute.contains("backend-upstream-source"));
		assertFalse(adminNginxRoute.contains("backend-upstream-target"));
		assertFalse(adminNginxRoute.contains("actuator-upstream-source"));
		assertFalse(adminNginxRoute.contains("actuator-upstream-target"));
		assertFalse(adminNginxRoute.contains("tasks_from: migrate_legacy_upstreams.yml"));
		assertFalse(adminNginxRoute.contains("legacy-upstream-source"));
		assertFalse(adminNginxRoute.contains("legacy-upstream-target"));
		assertFalse(adminNginxRoute.contains("verify-legacy-target-fragments"));
		assertFalse(adminNginxRoute.contains("sync-legacy-backend-upstream-target"));
		assertFalse(adminNginxRoute.contains("sync-legacy-admin-upstream-target"));
		assertFalse(adminNginxRoute.contains("sync-legacy-actuator-upstream-target"));
		assertFalse(adminNginxRoute.contains("remove-legacy-upstream-target-before-validation"));
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
		assertTrue(transaction.contains("nginx_fragment_transaction_file_pre_state: {}"));
		assertTrue(transaction.contains("item.affects_reload is boolean"));
		assertTrue(transaction.contains("mode: preserve"));
		assertTrue(transaction.contains("nginx_fragment_transaction_file_pre_state.get(item.id, {})"));
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
		assertFalse(nginxBaseConfig.contains("nginx_legacy_upstream_source_path"));
		assertFalse(nginxBaseConfig.contains("nginx_legacy_upstream_target_path"));
		assertFalse(nginxBaseConfig.contains("Abort unsafe legacy-target-only upstream migration"));
		String legacyTargetPlaceholderRefusal =
			"Refusing to replace legacy target upstream config with placeholder fragments.";
		assertFalse(nginxBaseConfig.contains(legacyTargetPlaceholderRefusal));
		assertFalse(nginxBaseConfig.contains("nginx_base_config_missing_upstream_sources"));
		assertFalse(nginxBaseConfig.contains("legacy-upstream-source"));
		assertFalse(nginxBaseConfig.contains("legacy-upstream-target"));
		assertFalse(nginxBaseConfig.contains("split-legacy-upstream-source"));
		assertFalse(nginxBaseConfig.contains("remove-legacy-upstream-target-before-validation"));
		assertTrue(nginxBaseConfig.contains("when_file_missing: backend-upstream-source"));
		assertTrue(readme.contains("nginx_base_config"));
		assertTrue(readme.contains("nginx_fragment_transaction_file_pre_state"));
		assertTrue(readme.contains("published file pre-state"));
		assertTrue(readme.contains("changed_if.stdout_json.changed: true"));
		String bluegreenTransactionFilesVar =
			"nginx_fragment_transaction_files: \"{{ app_bluegreen_nginx_transaction_files }}\"";
		String bluegreenTransactionOperationsVar =
			"nginx_fragment_transaction_operations: \"{{ app_bluegreen_nginx_transaction_operations }}\"";
		String bluegreenTransactionBackupCleanupTask =
			"Remove blue-green nginx transaction backup files after successful rollout";
		assertTrue(appBluegreenRunSwitch.contains("name: nginx_fragment_transaction"));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_id: app-bluegreen-switch"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_nginx_transaction_files:"));
		assertTrue(appBluegreenRunSwitch.contains(bluegreenTransactionFilesVar));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_files:"));
		assertTrue(appBluegreenRunSwitch.contains("app_bluegreen_nginx_transaction_operations:"));
		assertTrue(appBluegreenRunSwitch.contains(bluegreenTransactionOperationsVar));
		assertTrue(appBluegreenRunSwitch.contains("nginx_fragment_transaction_operations:"));
		assertFalse(appBluegreenRunSwitch.contains("nginx_fragment_transaction_validate_command:"));
		assertFalse(appBluegreenRunSwitch.contains("nginx_fragment_transaction_reload_command:"));
		assertTrue(appBluegreenRunSwitch.contains("stdout_json:"));
		assertFalse(appBluegreenRunSwitch.contains("stdout_contains: changed=true"));
		assertFalse(appBluegreenRunSwitch.contains("split-upstreams"));
		assertFalse(appBluegreenRunSwitch.contains("legacy-upstream-source"));
		assertFalse(appBluegreenRunSwitch.contains("legacy-upstream-target"));
		assertFalse(appBluegreenRunSwitch.contains("split-legacy-upstream-source"));
		assertFalse(appBluegreenRunSwitch.contains("admin-upstream-source"));
		assertFalse(appBluegreenRunSwitch.contains("admin-upstream-target"));
		assertFalse(appBluegreenRunSwitch.contains("sync-admin-upstream-target-for-legacy-migration"));
		assertFalse(appBluegreenRunSwitch.contains("remove-legacy-upstream-target-before-validation"));
		assertTrue(appBluegreenRunSwitch.contains("upsert-upstream"));
		assertFalse(appBluegreenRunSwitch.contains("Validate nginx config after upstream switch"));
		assertFalse(appBluegreenRunSwitch.contains("Reload nginx after upstream switch"));
		assertFalse(appBluegreenRunSwitch.contains("Restore previous nginx source config from backup"));
		assertTrue(appBluegreenRunSwitch.contains("cleanup_backup_on_success: false"));
		assertTrue(appBluegreenRunSwitch.contains("Restore blue-green nginx transaction files after failed rollout"));
		assertTrue(appBluegreenRunSwitch.contains("Remove blue-green nginx transaction files absent before failed rollout"));
		assertTrue(appBluegreenRunSwitch.contains(
			"Remove blue-green nginx transaction backup files after failed rollout restore"));
		assertTrue(appBluegreenRunSwitch.contains("post_failure_restore_validate_rc="));
		assertTrue(appBluegreenRunSwitch.contains(bluegreenTransactionBackupCleanupTask));
		assertTrue(adminNginxRoute.contains("name: nginx_fragment_transaction"));
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_files:"));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_files: \"{{ app_stopstart_admin_nginx_transaction_files }}\""));
		assertTrue(adminNginxRoute.contains("app_stopstart_admin_nginx_transaction_operations:"));
		assertTrue(adminNginxRoute.contains(
			"nginx_fragment_transaction_operations: \"{{ app_stopstart_admin_nginx_transaction_operations }}\""));
		assertFalse(adminNginxRoute.contains("nginx_fragment_transaction_validate_command:"));
		assertFalse(adminNginxRoute.contains("nginx_fragment_transaction_reload_command:"));
		assertTrue(adminNginxRoute.contains("stdout_json:"));
		assertFalse(adminNginxRoute.contains("stdout_contains: changed=true"));
		assertFalse(adminNginxRoute.contains("remove-legacy-upstream-target-before-validation"));
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

	@Test
	void observabilityBuildKeepsSentryRuntimeOnlyAndAvoidsSharedBoundaryLeaks() throws Exception {
		String observabilityBuild = read("observability/build.gradle.kts");
		String uncommented = stripLineComments(observabilityBuild);

		assertTrue(uncommented.contains("compileOnly(libs.spring.boot.starter.web)"));
		assertTrue(uncommented.contains("implementation(libs.kotlinx.coroutines.slf4j)"));
		assertTrue(uncommented.contains("implementation(libs.sentry.spring.boot.starter)"));
		assertTrue(uncommented.contains("runtimeOnly(libs.sentry.async.profiler)"));
		assertTrue(uncommented.contains("runtimeOnly(libs.sentry.log4j2)"));

		assertFalse(uncommented.contains("project(\":global-support\")"));
		assertFalse(uncommented.contains("libs.lombok"));
		assertFalse(uncommented.contains("annotationProcessor"));
		assertFalse(uncommented.contains("libs.spring.boot.starter.actuator"));
		assertFalse(uncommented.contains("libs.slf4j.api"));
		assertFalse(uncommented.contains("slf4j-api"));
	}

	@Test
	void staleDependencyBoundaryCatalogAliasesDoNotReturn() throws Exception {
		String catalog = read("gradle/libs.versions.toml");

		assertCatalogAliasAbsent(catalog, "versions", "awspring");
		assertCatalogAliasAbsent(catalog, "versions", "querydsl");
		assertCatalogAliasAbsent(catalog, "versions", "slf4j");
		assertCatalogAliasAbsent(catalog, "plugins", "spring-boot");
		assertCatalogAliasAbsent(catalog, "plugins", "spring-dependency-management");
		assertCatalogAliasAbsent(catalog, "plugins", "kotlin-jvm");
		assertCatalogAliasAbsent(catalog, "plugins", "kotlin-spring");
		assertCatalogAliasAbsent(catalog, "plugins", "kotlin-jpa");
		assertCatalogAliasAbsent(catalog, "plugins", "sentry-jvm");
		assertCatalogAliasAbsent(catalog, "libraries", "awspring-cloud-aws-starter-s3");
		assertCatalogAliasAbsent(catalog, "libraries", "querydsl-jpa");
		assertCatalogAliasAbsent(catalog, "libraries", "querydsl-apt");
		assertCatalogAliasAbsent(catalog, "libraries", "spring-security-core");
		assertCatalogAliasAbsent(catalog, "libraries", "slf4j-api");
		assertCatalogAliasAbsent(catalog, "bundles", "test-common");
		assertCatalogAliasAbsent(catalog, "bundles", "web-app-god");
	}

	@Test
	void versionCatalogCheckerDoesNotTreatCommentedGradleAccessorsAsUsage(@TempDir Path tempRoot) throws Exception {
		Path gradleDir = tempRoot.resolve("gradle");
		Files.createDirectories(gradleDir);
		Files.writeString(gradleDir.resolve("libs.versions.toml"), """
			[versions]
			used = "1.0.0"
			lookup = "1.0.0"
			unused = "1.0.0"

			[libraries]
			used-lib = { module = "com.example:used", version.ref = "used" }
			lookup-lib = { module = "com.example:lookup", version.ref = "lookup" }
			unused-lib = { module = "com.example:unused", version.ref = "unused" }
			""".stripIndent());
		Files.writeString(tempRoot.resolve("build.gradle.kts"), String.join("\n",
			"// implementation(libs.unused.lib)",
			"val stringMention = \"libs.unused.lib\"",
			"val multilineMention = \"\"\"",
			"    libs.unused.lib",
			"\"\"\"",
			"dependencies {",
			"    implementation(libs.used.lib)",
			"    implementation(libs.findLibrary(\"lookup-lib\").get())",
			"    /*",
			"     * implementation(libs.unused.lib)",
			"     */",
			"}"
		));

		Path checker = Path.of(".github/scripts/check_unused_version_catalog_aliases.py")
			.toAbsolutePath()
			.normalize();
		Process process = new ProcessBuilder("python3", checker.toString(), "--root", tempRoot.toString())
			.redirectErrorStream(true)
			.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exitCode = process.waitFor();

		assertEquals(1, exitCode, output);
		assertTrue(output.contains("libraries.unused-lib"), output);
		assertTrue(output.contains("versions.unused"), output);
	}

	private static String read(String path) throws IOException {
		return Files.readString(Path.of(path));
	}

	private static String sectionAfter(String content, String marker) {
		int start = content.indexOf(marker);
		assertTrue(start >= 0, marker);
		int nextDocument = content.indexOf("\n---", start + marker.length());
		if (nextDocument < 0) {
			return content.substring(start);
		}
		return content.substring(start, nextDocument);
	}

	private static String sectionBetween(String content, String startMarker, String endMarker) {
		int start = content.indexOf(startMarker);
		assertTrue(start >= 0, startMarker);
		int end = content.indexOf(endMarker, start + startMarker.length());
		assertTrue(end >= 0, endMarker);
		return content.substring(start, end);
	}

	private static int countOccurrences(String content, String needle) {
		int count = 0;
		int index = 0;
		while ((index = content.indexOf(needle, index)) >= 0) {
			count++;
			index += needle.length();
		}
		return count;
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

	private static void assertCatalogAliasAbsent(String catalog, String section, String alias) {
		String sectionBody = sectionBody(catalog, section);
		assertFalse(sectionBody.matches("(?ms).*^" + java.util.regex.Pattern.quote(alias) + "\\s*=.*"),
			"gradle/libs.versions.toml must not reintroduce " + section + "." + alias);
	}

	private static String sectionBody(String catalog, String section) {
		String marker = "[" + section + "]";
		int start = catalog.indexOf(marker);
		assertTrue(start >= 0, marker);
		int next = catalog.indexOf("\n[", start + marker.length());
		return next < 0 ? catalog.substring(start) : catalog.substring(start, next);
	}

	private static String stripLineComments(String source) {
		return source.replaceAll("(?m)//.*$", "");
	}

	private static void assertBefore(String content, String first, String second) {
		int firstIndex = content.indexOf(first);
		int secondIndex = content.indexOf(second);
		assertTrue(firstIndex >= 0, first);
		assertTrue(secondIndex >= 0, second);
		assertTrue(firstIndex < secondIndex, first + " should appear before " + second);
	}
}
