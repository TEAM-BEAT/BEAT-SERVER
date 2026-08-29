import { getWorkloadBudget, WORKLOAD_BUDGET_VERSION } from './budgets.js';

const TARGET_ENVIRONMENTS = Object.freeze({
  dev: Object.freeze({
    allowedHosts: Object.freeze(['api-dev.beatlive.kr']),
    serverType: 't4g.small',
  }),
  prod: Object.freeze({
    allowedHosts: Object.freeze(['api.beatlive.kr']),
    serverType: 't4g.small',
  }),
});

const LOAD_BUDGET_OVERRIDE_ENVIRONMENTS = Object.freeze([
  'TARGET_RPS',
  'DURATION',
  'PRE_ALLOCATED_VUS',
  'MAX_VUS',
  'GRACEFUL_STOP',
]);

function requireValue(env, name) {
  const value = env[name];
  if (value === undefined || value === null || String(value).trim() === '') {
    throw new Error(`${name} is required.`);
  }
  return String(value).trim();
}

function positiveNumber(value, name) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${name} must be a positive number.`);
  }
  return parsed;
}

function safeIdentifier(value, name, fallback) {
  const normalized = String(value || fallback).trim();
  if (!/^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$/.test(normalized)) {
    throw new Error(`${name} must contain only safe identifier characters.`);
  }
  return normalized;
}

function profileFromEnv(env) {
  if (env.LOAD_PROFILE && env.PROFILE && env.LOAD_PROFILE !== env.PROFILE) {
    throw new Error('LOAD_PROFILE and PROFILE must match when both are provided.');
  }
  return String(env.LOAD_PROFILE || env.PROFILE || 'smoke').trim();
}

function validateBaseUrl(value, targetEnvironment) {
  const match = /^https:\/\/([^/?#]+)(\/[^?#]*)?(\?[^#]*)?(#.*)?$/.exec(value);
  if (!match) {
    throw new Error('BASE_URL must be a valid HTTPS origin.');
  }

  const authority = match[1];
  const hostname = authority.toLowerCase();
  if (
    authority.includes('@')
    || authority.includes(':')
    || (match[2] && match[2] !== '/')
    || match[3]
    || match[4]
  ) {
    throw new Error('BASE_URL must contain only an HTTPS origin without credentials or a path.');
  }
  if (!targetEnvironment.allowedHosts.includes(hostname)) {
    throw new Error('BASE_URL host is not allowlisted for TARGET_ENV.');
  }
  return `https://${hostname}`;
}

function validateRuntimeBudgetOverrides(env) {
  const override = LOAD_BUDGET_OVERRIDE_ENVIRONMENTS.find((name) => env[name] !== undefined);
  if (override) {
    throw new Error(
      `${override} is controlled by the versioned LOAD_PROFILE budget; choose LOAD_PROFILE instead.`,
    );
  }
}

function validateDatasetHash(value) {
  if (value !== undefined && !/^[a-f0-9]{64}$/i.test(value)) {
    throw new Error('DATASET_HASH must be a SHA-256 hex digest.');
  }
  return value ? value.toLowerCase() : null;
}

function validateSummaryFile(value) {
  const summaryFile = String(value).trim();
  if (
    summaryFile.length > 255
    || summaryFile.includes('/')
    || summaryFile.includes('\\')
    || summaryFile.includes('\0')
    || !/^[A-Za-z0-9][A-Za-z0-9._:-]*\.json$/.test(summaryFile)
  ) {
    throw new Error('SUMMARY_FILE must be a safe JSON basename without path separators.');
  }
  return summaryFile;
}

function buildTags(config) {
  return Object.freeze({
    test_id: config.testId,
    git_sha: config.gitSha,
    scenario: config.scenario,
    dataset_hash: config.datasetHash,
    target_env: config.targetEnv,
    environment: config.targetEnv,
    server_type: config.serverType,
    server_instance_type: config.serverType,
    load_profile: config.profile,
    budget_version: config.budgetVersion,
  });
}

export function durationInSeconds(value) {
  const match = /^(\d+)(s|m)$/.exec(value);
  if (!match) {
    throw new Error('DURATION must use seconds or minutes, for example 30s or 2m.');
  }
  return Number(match[1]) * (match[2] === 'm' ? 60 : 1);
}

export function loadConfig(env, defaults = {}) {
  const targetEnv = requireValue(env, 'TARGET_ENV');
  const targetEnvironment = TARGET_ENVIRONMENTS[targetEnv];
  if (!targetEnvironment) {
    throw new Error('TARGET_ENV must be exactly dev or prod.');
  }

  validateRuntimeBudgetOverrides(env);

  const scenario = defaults.scenario || 'generic_http';
  if (env.SCENARIO && env.SCENARIO !== scenario) {
    throw new Error(`SCENARIO is fixed to ${scenario} for this script.`);
  }
  const profile = profileFromEnv(env);
  const budget = getWorkloadBudget(scenario, profile);
  const baseUrl = validateBaseUrl(requireValue(env, 'BASE_URL'), targetEnvironment);
  const preflightPath = env.PREFLIGHT_PATH || '/api/main';
  if (!preflightPath.startsWith('/') || preflightPath.startsWith('//')) {
    throw new Error('PREFLIGHT_PATH must be an origin-relative path.');
  }

  let maxP95Ms = null;
  if (env.MAX_P95_MS) {
    maxP95Ms = positiveNumber(env.MAX_P95_MS, 'MAX_P95_MS');
  }

  const preflightExpectedStatus = Number(env.PREFLIGHT_EXPECTED_STATUS || 200);
  if (!Number.isInteger(preflightExpectedStatus) || preflightExpectedStatus < 100 || preflightExpectedStatus > 599) {
    throw new Error('PREFLIGHT_EXPECTED_STATUS must be a valid HTTP status.');
  }

  const testId = safeIdentifier(env.TEST_ID, 'TEST_ID', `${scenario}-${profile}-${Date.now()}`);
  const gitSha = safeIdentifier(
    env.GIT_SHA || env.GIT_COMMIT_SHA || env.GITHUB_SHA,
    'GIT_SHA',
    'unknown',
  );
  const serverType = safeIdentifier(
    env.SERVER_TYPE,
    'SERVER_TYPE',
    targetEnvironment.serverType,
  );
  const datasetHash = validateDatasetHash(env.DATASET_HASH);
  const summaryFile = validateSummaryFile(env.SUMMARY_FILE || `summary-${testId}.json`);

  const config = {
    accessToken: env.ACCESS_TOKEN || '',
    baseUrl,
    budget,
    budgetVersion: WORKLOAD_BUDGET_VERSION,
    datasetHash,
    duration: budget.duration,
    durationSeconds: budget.durationSeconds,
    gitSha,
    gracefulStop: budget.gracefulStop,
    maxP95Ms,
    maxVUs: budget.maxVUs,
    peakRps: budget.peakRps,
    plannedIterations: budget.plannedIterations,
    preAllocatedVUs: budget.preAllocatedVUs,
    preflightExpectedStatus,
    preflightUrl: `${baseUrl}${preflightPath}`,
    profile,
    requestTimeout: env.REQUEST_TIMEOUT || defaults.requestTimeout || '10s',
    scenario,
    serverType,
    summaryFile,
    targetEnv,
    targetRps: budget.targetRps,
    testId,
  };
  return Object.freeze({ ...config, tags: datasetHash ? buildTags(config) : null });
}

export function withDatasetHash(config, actualDatasetHash) {
  const datasetHash = validateDatasetHash(actualDatasetHash);
  if (!datasetHash) {
    throw new Error('A dataset SHA-256 hash is required.');
  }
  if (config.datasetHash && config.datasetHash !== datasetHash) {
    throw new Error('DATASET_HASH does not match the loaded workload data.');
  }
  const nextConfig = { ...config, datasetHash };
  return Object.freeze({ ...nextConfig, tags: buildTags(nextConfig) });
}

export function addOptionalP95Threshold(thresholds, metric, maxP95Ms) {
  if (maxP95Ms !== null) {
    thresholds[metric] = [`p(95)<${maxP95Ms}`];
  }
  return thresholds;
}

export function addAbortableRateThreshold(thresholds, metric, threshold = 'rate<0.05') {
  thresholds[metric] = [
    {
      threshold,
      abortOnFail: true,
      delayAbortEval: '30s',
    },
  ];
  return thresholds;
}

export function addDroppedIterationsThreshold(thresholds) {
  thresholds.dropped_iterations = [
    {
      threshold: 'count==0',
      abortOnFail: true,
      delayAbortEval: '0s',
    },
  ];
  return thresholds;
}

export function constantArrivalRateScenario(workload, config) {
  if (config.budget.executor === 'ramping-arrival-rate') {
    return {
      executor: 'ramping-arrival-rate',
      startRate: config.budget.startRate,
      timeUnit: '1s',
      stages: config.budget.stages,
      preAllocatedVUs: config.preAllocatedVUs,
      maxVUs: config.maxVUs,
      gracefulStop: config.gracefulStop,
      tags: { workload },
    };
  }

  return {
    executor: 'constant-arrival-rate',
    rate: config.targetRps,
    timeUnit: '1s',
    duration: config.duration,
    preAllocatedVUs: config.preAllocatedVUs,
    maxVUs: config.maxVUs,
    gracefulStop: config.gracefulStop,
    tags: { workload },
  };
}
