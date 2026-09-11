export const STOCK_CONTENTION_SCHEMA_VERSION = 'v3';

export const STOCK_CONTENTION_PHASES = Object.freeze(['warmup', 'flash']);

export const STOCK_CONTENTION_CASE_COUNTS = Object.freeze({
  warmup: 900,
  flash: 200,
});

export const STOCK_CONTENTION_TOTAL_CASE_COUNT =
  STOCK_CONTENTION_CASE_COUNTS.warmup + STOCK_CONTENTION_CASE_COUNTS.flash;

export const STOCK_CONTENTION_STRATEGIES = Object.freeze([
  'PESSIMISTIC',
  'OPTIMISTIC',
  'REDIS',
  'ATOMIC',
]);

// The runner uses one order per repetition block so that strategy position is
// not confounded with warm shared state.
export const STOCK_CONTENTION_STRATEGY_ROTATIONS = Object.freeze([
  Object.freeze(['PESSIMISTIC', 'OPTIMISTIC', 'REDIS', 'ATOMIC']),
  Object.freeze(['OPTIMISTIC', 'REDIS', 'ATOMIC', 'PESSIMISTIC']),
  Object.freeze(['REDIS', 'ATOMIC', 'PESSIMISTIC', 'OPTIMISTIC']),
  Object.freeze(['ATOMIC', 'PESSIMISTIC', 'OPTIMISTIC', 'REDIS']),
  Object.freeze(['PESSIMISTIC', 'REDIS', 'ATOMIC', 'OPTIMISTIC']),
]);

export const STOCK_CONTENTION_REPETITIONS = 5;
export const STOCK_CONTENTION_QUIET_PERIOD_SECONDS = 60;

export const STOCK_CONTENTION_EXPERIMENT_PATH = '/api/internal/experiments/stock-contention';

export const STOCK_CONTENTION_REQUEST_TIMEOUT = '35s';

const EC2_INSTANCE_ID_PATTERN = /^i-[0-9a-f]{8,17}$/;
const EC2_CPU_CREDIT_MODES = new Set(['standard', 'unlimited']);

export const STOCK_CONTENTION_RESPONSE_OUTCOMES = Object.freeze([
  'ACCEPTED',
  'SOLD_OUT',
  'CONFLICT_EXHAUSTED',
  'LOCK_TIMEOUT',
]);

export const STOCK_CONTENTION_OUTCOMES = Object.freeze([
  'submitted',
  'accepted',
  'sold_out',
  'conflict_exhausted',
  'lock_timeout',
  'unexpected',
]);

export const SOLD_OUT_ERROR_CODE = 'SCHEDULE_INSUFFICIENT_TICKETS';

const DATASET_KEYS = new Set([
  'schema_version',
  'accessToken',
  'warmupScheduleId',
  'flashScheduleId',
  'bookerName',
  'bookerPhoneNumber',
]);
const PHASE_SET = new Set(STOCK_CONTENTION_PHASES);
const STRATEGY_SET = new Set(STOCK_CONTENTION_STRATEGIES);
const OUTCOME_SET = new Set(STOCK_CONTENTION_OUTCOMES);
const RESPONSE_OUTCOME_SET = new Set(STOCK_CONTENTION_RESPONSE_OUTCOMES);
const BOOKER_NAME_PATTERN = /^[a-zA-Z가-힣]+$/;
const PHONE_PATTERN = /^\d{3}-\d{4}-\d{4}$/;

function requireObject(value, description) {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${description} must be an object.`);
  }
}

function rejectUnknownKeys(value, allowedKeys, description) {
  Object.keys(value).forEach((key) => {
    if (!allowedKeys.has(key)) {
      throw new Error(`${description} contains an unsupported field.`);
    }
  });
}

export function validateStockCases(dataset) {
  requireObject(dataset, 'Stock contention dataset');
  rejectUnknownKeys(dataset, DATASET_KEYS, 'Stock contention dataset');
  if (dataset.schema_version !== STOCK_CONTENTION_SCHEMA_VERSION) {
    throw new Error(`Stock contention dataset schema_version must be ${STOCK_CONTENTION_SCHEMA_VERSION}.`);
  }
  if (
    typeof dataset.accessToken !== 'string'
    || dataset.accessToken.length === 0
    || dataset.accessToken.trim() !== dataset.accessToken
    || /\s/.test(dataset.accessToken)
  ) {
    throw new Error('Stock contention dataset must contain a non-empty accessToken.');
  }

  if (!Number.isInteger(dataset.warmupScheduleId) || dataset.warmupScheduleId < 1) {
    throw new Error('Stock contention dataset has an invalid warmupScheduleId.');
  }
  if (!Number.isInteger(dataset.flashScheduleId) || dataset.flashScheduleId < 1) {
    throw new Error('Stock contention dataset has an invalid flashScheduleId.');
  }
  if (dataset.warmupScheduleId === dataset.flashScheduleId) {
    throw new Error('Warmup and flash scheduleId values must be different.');
  }
  if (typeof dataset.bookerName !== 'string' || !BOOKER_NAME_PATTERN.test(dataset.bookerName)) {
    throw new Error('Stock contention dataset has an invalid bookerName.');
  }
  if (
    typeof dataset.bookerPhoneNumber !== 'string'
    || !PHONE_PATTERN.test(dataset.bookerPhoneNumber)
  ) {
    throw new Error('Stock contention dataset has an invalid bookerPhoneNumber.');
  }

  return Object.freeze({
    accessToken: dataset.accessToken,
    warmupScheduleId: dataset.warmupScheduleId,
    flashScheduleId: dataset.flashScheduleId,
    bookerName: dataset.bookerName,
    bookerPhoneNumber: dataset.bookerPhoneNumber,
    totalCases: STOCK_CONTENTION_TOTAL_CASE_COUNT,
  });
}

export function buildStockContentionRequest(validatedDataset, phase) {
  if (!PHASE_SET.has(phase)) {
    throw new Error('Invalid stock contention phase.');
  }
  return Object.freeze({
    scheduleId: phase === 'warmup'
      ? validatedDataset.warmupScheduleId
      : validatedDataset.flashScheduleId,
    purchaseTicketCount: 1,
    bookerName: validatedDataset.bookerName,
    bookerPhoneNumber: validatedDataset.bookerPhoneNumber,
  });
}

export function assertDevOnlyTarget(targetEnv) {
  if (targetEnv !== 'dev') {
    throw new Error('stock_contention is dev-only; TARGET_ENV must be dev.');
  }
}

export function validateStrategy(strategy) {
  if (!STRATEGY_SET.has(strategy)) {
    throw new Error('STRATEGY must be PESSIMISTIC, OPTIMISTIC, REDIS, or ATOMIC.');
  }
  return strategy;
}

export function validateExperimentHostMetadata(env) {
  const ec2InstanceId = String(env.EC2_INSTANCE_ID || '').trim();
  const ec2CpuCredits = String(env.EC2_CPU_CREDITS || '').trim().toLowerCase();
  if (!EC2_INSTANCE_ID_PATTERN.test(ec2InstanceId)) {
    throw new Error('EC2_INSTANCE_ID must be the actual dev EC2 instance ID.');
  }
  if (!EC2_CPU_CREDIT_MODES.has(ec2CpuCredits)) {
    throw new Error('EC2_CPU_CREDITS must be exactly standard or unlimited.');
  }
  return Object.freeze({ ec2InstanceId, ec2CpuCredits });
}

export function stockContentionBookingPath(strategy) {
  return `${STOCK_CONTENTION_EXPERIMENT_PATH}/${validateStrategy(strategy)}/bookings`;
}

export function assertStockContentionTimeout(requestTimeout) {
  if (requestTimeout !== STOCK_CONTENTION_REQUEST_TIMEOUT) {
    throw new Error(
      `stock_contention request timeout is fixed to ${STOCK_CONTENTION_REQUEST_TIMEOUT}.`,
    );
  }
  return requestTimeout;
}

function parseBody(body) {
  if (body && typeof body === 'object' && !Array.isArray(body)) {
    return body;
  }
  if (typeof body !== 'string' || body.trim() === '') {
    return null;
  }
  try {
    const parsed = JSON.parse(body);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function hasOwn(value, key) {
  return Object.prototype.hasOwnProperty.call(value, key);
}

export function parseBookingResult(body) {
  const result = parseBody(body);
  if (
    !result
    || !hasOwn(result, 'outcome')
    || !RESPONSE_OUTCOME_SET.has(result.outcome)
    || !hasOwn(result, 'bookingId')
    || !hasOwn(result, 'attemptCount')
  ) {
    return null;
  }
  if (!Number.isInteger(result.attemptCount) || result.attemptCount < 0) {
    return null;
  }
  if (result.outcome === 'ACCEPTED' && result.attemptCount < 1) {
    return null;
  }
  if (
    result.bookingId !== null
    && (!Number.isInteger(result.bookingId) || result.bookingId < 1)
  ) {
    return null;
  }
  if (result.outcome === 'ACCEPTED' && result.bookingId === null) {
    return null;
  }
  if (result.outcome !== 'ACCEPTED' && result.bookingId !== null) {
    return null;
  }
  return {
    outcome: result.outcome,
    bookingId: result.bookingId,
    attemptCount: result.attemptCount,
  };
}

export function classifyBookingResponse({ status, body, phase }) {
  const result = parseBookingResult(body);
  if (status === 201 && result?.outcome === 'ACCEPTED' && result.bookingId !== null) {
    return 'accepted';
  }
  if (phase === 'flash' && status === 200 && result?.outcome === 'SOLD_OUT' && result.bookingId === null) {
    return 'sold_out';
  }
  if (status === 200 && result?.outcome === 'CONFLICT_EXHAUSTED') {
    return 'conflict_exhausted';
  }
  if (status === 200 && result?.outcome === 'LOCK_TIMEOUT') {
    return 'lock_timeout';
  }
  return 'unexpected';
}

export function isTimeoutStatus(status) {
  return status === 408 || status === 504;
}

export function buildStockMetricTags({ testId, gitSha, strategy, phase, outcome }) {
  if (typeof testId !== 'string' || typeof gitSha !== 'string') {
    throw new Error('testId and gitSha are required for stock contention metric tags.');
  }
  if (!PHASE_SET.has(phase)) {
    throw new Error('Invalid stock contention metric phase.');
  }
  if (!STRATEGY_SET.has(strategy)) {
    throw new Error('Invalid stock contention metric strategy.');
  }
  if (outcome !== undefined && !OUTCOME_SET.has(outcome)) {
    throw new Error('Invalid stock contention metric outcome.');
  }

  const tags = { test_id: testId, git_sha: gitSha, strategy, phase };
  if (outcome !== undefined) {
    tags.outcome = outcome;
  }
  return Object.freeze(tags);
}

export function expectedOutcomeCounts(phase) {
  if (phase === 'warmup') {
    return Object.freeze({ accepted: 900, sold_out: 0 });
  }
  if (phase === 'flash') {
    return Object.freeze({ accepted: 100, sold_out: 100 });
  }
  throw new Error('Invalid stock contention phase.');
}

export function counterMetricCount(values) {
  if (values === null || typeof values !== 'object') {
    return 0;
  }
  return Number.isFinite(values.count) ? values.count : 0;
}

export function exactOutcomeThresholds(phase) {
  const expected = expectedOutcomeCounts(phase);
  return Object.freeze({
    accepted: `count==${expected.accepted}`,
    sold_out: `count==${expected.sold_out}`,
    unexpected: 'count==0',
  });
}

export function isPlannedStockContentionIteration(iterationInTest, phaseCaseCount) {
  return Number.isInteger(iterationInTest)
    && iterationInTest >= 0
    && Number.isInteger(phaseCaseCount)
    && phaseCaseCount > 0
    && iterationInTest < phaseCaseCount;
}
