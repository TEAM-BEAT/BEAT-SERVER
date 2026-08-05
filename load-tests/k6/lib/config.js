const REQUIRED_ACKNOWLEDGEMENT = 'rds';
const REQUIRED_BOOKING_BLOCK_ACKNOWLEDGEMENT = 'confirmed';
const ABSOLUTE_MAX_RPS = 2;
const ABSOLUTE_MAX_DURATION_SECONDS = 300;

function requireValue(env, name) {
  const value = env[name];
  if (!value) {
    throw new Error(`${name} is required.`);
  }
  return value;
}

function positiveInteger(value, name) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new Error(`${name} must be a positive integer.`);
  }
  return parsed;
}

export function durationInSeconds(value) {
  const match = /^(\d+)(s|m)$/.exec(value);
  if (!match) {
    throw new Error('DURATION must use seconds or minutes, for example 30s or 2m.');
  }
  return Number(match[1]) * (match[2] === 'm' ? 60 : 1);
}

export function loadSafeProdConfig(env, defaults = {}) {
  if (env.LOAD_TEST_ACK !== REQUIRED_ACKNOWLEDGEMENT) {
    throw new Error(
      `Set LOAD_TEST_ACK=${REQUIRED_ACKNOWLEDGEMENT} after confirming live bookings are blocked and the RDS load-test risk.`,
    );
  }
  if (env.TARGET_ENV !== 'prod') {
    throw new Error('TARGET_ENV must be prod.');
  }
  if (env.LIVE_BOOKING_BLOCKED_ACK !== REQUIRED_BOOKING_BLOCK_ACKNOWLEDGEMENT) {
    throw new Error(
      `Set LIVE_BOOKING_BLOCKED_ACK=${REQUIRED_BOOKING_BLOCK_ACKNOWLEDGEMENT} after blocking live bookings.`,
    );
  }

  const baseUrl = requireValue(env, 'BASE_URL').replace(/\/+$/, '');
  const allowedOrigin = requireValue(env, 'ALLOWED_ORIGIN').replace(/\/+$/, '');
  if (!baseUrl.startsWith('https://') || baseUrl !== allowedOrigin) {
    throw new Error('BASE_URL must exactly match the approved HTTPS ALLOWED_ORIGIN.');
  }

  const preflightPath = env.PREFLIGHT_PATH || '/api/main';
  if (!preflightPath.startsWith('/') || preflightPath.startsWith('//')) {
    throw new Error('PREFLIGHT_PATH must be an origin-relative path.');
  }

  const targetRps = positiveInteger(env.TARGET_RPS || 1, 'TARGET_RPS');
  if (targetRps > ABSOLUTE_MAX_RPS) {
    throw new Error(`TARGET_RPS must not exceed the absolute cap(${ABSOLUTE_MAX_RPS}).`);
  }

  const duration = env.DURATION || '1m';
  const durationSeconds = durationInSeconds(duration);
  if (durationSeconds > ABSOLUTE_MAX_DURATION_SECONDS) {
    throw new Error(`DURATION must not exceed ${ABSOLUTE_MAX_DURATION_SECONDS} seconds.`);
  }

  const preAllocatedVUs = positiveInteger(
    env.PRE_ALLOCATED_VUS || defaults.preAllocatedVUs || 10,
    'PRE_ALLOCATED_VUS',
  );
  const maxVUs = positiveInteger(env.MAX_VUS || defaults.maxVUs || 50, 'MAX_VUS');
  if (maxVUs < preAllocatedVUs) {
    throw new Error('MAX_VUS must be greater than or equal to PRE_ALLOCATED_VUS.');
  }

  let maxP95Ms = null;
  if (env.MAX_P95_MS) {
    maxP95Ms = Number(env.MAX_P95_MS);
    if (!Number.isFinite(maxP95Ms) || maxP95Ms <= 0) {
      throw new Error('MAX_P95_MS must be a positive number.');
    }
  }

  const preflightExpectedStatus = Number(env.PREFLIGHT_EXPECTED_STATUS || 200);
  if (!Number.isInteger(preflightExpectedStatus) || preflightExpectedStatus < 100 || preflightExpectedStatus > 599) {
    throw new Error('PREFLIGHT_EXPECTED_STATUS must be a valid HTTP status.');
  }

  return Object.freeze({
    accessToken: env.ACCESS_TOKEN || '',
    baseUrl,
    duration,
    durationSeconds,
    maxP95Ms,
    maxVUs,
    preAllocatedVUs,
    preflightExpectedStatus,
    preflightUrl: `${baseUrl}${preflightPath}`,
    requestTimeout: env.REQUEST_TIMEOUT || defaults.requestTimeout || '10s',
    targetRps,
  });
}

export function constantArrivalRateScenario(workload, config, overrides = {}) {
  return {
    executor: 'constant-arrival-rate',
    rate: config.targetRps,
    timeUnit: '1s',
    duration: config.duration,
    preAllocatedVUs: config.preAllocatedVUs,
    maxVUs: config.maxVUs,
    gracefulStop: overrides.gracefulStop || '30s',
    tags: { workload },
  };
}

export function addOptionalP95Threshold(thresholds, metric, maxP95Ms) {
  if (maxP95Ms !== null) {
    thresholds[metric] = [`p(95)<${maxP95Ms}`];
  }
  return thresholds;
}
