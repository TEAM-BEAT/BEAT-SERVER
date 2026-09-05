export function handleSummaryWithMetadata(data, config, extraMetadata = {}) {
  const summary = {
    schema_version: 'v1',
    generated_at: new Date().toISOString(),
    metadata: {
      test_id: config.testId,
      git_sha: config.gitSha,
      scenario: config.scenario,
      dataset_hash: config.datasetHash,
      target_env: config.targetEnv,
      server_type: config.serverType,
      ...extraMetadata,
    },
    workload: {
      profile: config.profile,
      budget_version: config.budgetVersion,
      duration: config.duration,
      peak_rps: config.peakRps,
      planned_iterations: config.plannedIterations,
    },
    metrics: data.metrics,
    root_group: data.root_group,
  };

  return {
    [config.summaryFile]: JSON.stringify(summary, null, 2),
  };
}
