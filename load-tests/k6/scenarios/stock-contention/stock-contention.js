import { check } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';
import { Counter, Rate, Trend } from 'k6/metrics';
import {
  addDroppedIterationsThreshold,
  addOptionalP95Threshold,
  constantArrivalRateScenario,
  loadConfig,
  withDatasetHash,
} from '../../lib/config.js';
import {
  assertDevOnlyTarget,
  assertStockContentionTimeout,
  buildStockMetricTags,
  classifyBookingResponse,
  counterMetricCount,
  exactOutcomeThresholds,
  expectedOutcomeCounts,
  isTimeoutStatus,
  parseBookingResult,
  stockContentionBookingPath,
  STOCK_CONTENTION_REQUEST_TIMEOUT,
  validateStrategy,
} from './contract.js';
import {
  assertPreflight,
  authorizationHeaders,
  isTimeoutResponse,
} from '../../lib/http.js';
import { handleSummaryWithMetadata } from '../../lib/summary.js';
import { loadCases } from './cases.js';

const initialConfig = loadConfig(__ENV, {
  scenario: 'stock_contention',
  requestTimeout: STOCK_CONTENTION_REQUEST_TIMEOUT,
  includeProfileInSummaryFile: true,
});
assertDevOnlyTarget(initialConfig.targetEnv);
assertStockContentionTimeout(initialConfig.requestTimeout);
if (initialConfig.accessToken) {
  throw new Error('stock_contention requires one accessToken per case; ACCESS_TOKEN is not allowed.');
}

const strategy = validateStrategy(__ENV.STRATEGY);
const bookingPath = stockContentionBookingPath(strategy);
const loadedCases = loadCases(__ENV, initialConfig);
const { cases } = loadedCases;
const config = Object.freeze({
  ...withDatasetHash(initialConfig, loadedCases.datasetHash),
  strategy,
});
const phase = config.profile;
const expected = expectedOutcomeCounts(phase);
const metricContext = {
  testId: config.testId,
  gitSha: config.gitSha,
  strategy,
  phase,
};

const REQUESTS_SUBMITTED_METRIC = 'stock_contention_requests_submitted';
const BOOKINGS_ACCEPTED_METRIC = 'stock_contention_bookings_accepted';
const BOOKINGS_SOLD_OUT_METRIC = 'stock_contention_bookings_sold_out';
const CONFLICT_EXHAUSTED_METRIC = 'stock_contention_conflict_exhausted';
const LOCK_TIMEOUT_METRIC = 'stock_contention_lock_timeout';
const UNEXPECTED_RESPONSES_METRIC = 'stock_contention_unexpected_response';
const REQUEST_TIMEOUT_METRIC = 'stock_contention_request_timeout';
const TIMEOUTS_METRIC = 'stock_contention_timeouts';
const ACCEPTED_LATENCY_METRIC = 'stock_contention_accepted_latency_ms';
const TERMINAL_LATENCY_METRIC = 'stock_contention_terminal_latency_ms';
const REQUEST_START_ELAPSED_METRIC = 'stock_contention_request_start_elapsed_ms';
const COMPLETION_ELAPSED_METRIC = 'stock_contention_completion_elapsed_ms';
const ATTEMPT_COUNT_METRIC = 'stock_contention_attempt_count';

const requestsSubmitted = new Counter(REQUESTS_SUBMITTED_METRIC);
const bookingsAccepted = new Counter(BOOKINGS_ACCEPTED_METRIC);
const bookingsSoldOut = new Counter(BOOKINGS_SOLD_OUT_METRIC);
const conflictsExhausted = new Counter(CONFLICT_EXHAUSTED_METRIC);
const lockTimeouts = new Counter(LOCK_TIMEOUT_METRIC);
const unexpectedResponses = new Counter(UNEXPECTED_RESPONSES_METRIC);
const requestTimedOut = new Rate(REQUEST_TIMEOUT_METRIC);
const timeouts = new Counter(TIMEOUTS_METRIC);
const acceptedLatency = new Trend(ACCEPTED_LATENCY_METRIC);
const terminalLatency = new Trend(TERMINAL_LATENCY_METRIC);
const requestStartElapsed = new Trend(REQUEST_START_ELAPSED_METRIC);
const completionElapsed = new Trend(COMPLETION_ELAPSED_METRIC);
const attemptCount = new Trend(ATTEMPT_COUNT_METRIC);

const exactThresholds = exactOutcomeThresholds(phase);
const thresholds = {
  'checks{name:stock_contention_response_recognized}': ['rate==1'],
  [BOOKINGS_ACCEPTED_METRIC]: [exactThresholds.accepted],
  [BOOKINGS_SOLD_OUT_METRIC]: [exactThresholds.sold_out],
  [CONFLICT_EXHAUSTED_METRIC]: ['count==0'],
  [LOCK_TIMEOUT_METRIC]: ['count==0'],
  [UNEXPECTED_RESPONSES_METRIC]: [exactThresholds.unexpected],
  [REQUEST_TIMEOUT_METRIC]: ['rate==0'],
};
addDroppedIterationsThreshold(thresholds);
addOptionalP95Threshold(thresholds, ACCEPTED_LATENCY_METRIC, config.maxP95Ms);

export const options = {
  // The response body carries the outcome and attemptCount contract.
  discardResponseBodies: false,
  tags: { ...config.tags, strategy },
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error_code'],
  scenarios: {
    stock_contention: constantArrivalRateScenario('stock_contention', config),
  },
  thresholds,
};

export function setup() {
  assertPreflight(config);
  return { startedAt: Date.now() };
}

function metricValues(data, name) {
  return data.metrics?.[name]?.values || null;
}

function metricCount(data, name) {
  return counterMetricCount(metricValues(data, name));
}

function finiteValue(value) {
  return Number.isFinite(value) ? value : null;
}

function percentile(values, percentileValue) {
  if (!values) {
    return null;
  }
  const key = `p(${percentileValue})`;
  if (Number.isFinite(values[key])) {
    return values[key];
  }
  const alternateKey = `p${percentileValue}`;
  if (Number.isFinite(values[alternateKey])) {
    return values[alternateKey];
  }
  return percentileValue === 50 && Number.isFinite(values.med) ? values.med : null;
}

function latencySummary(values, fallbackCount = null) {
  return {
    count: finiteValue(values?.count) ?? fallbackCount,
    p50: percentile(values, 50),
    p95: percentile(values, 95),
    p99: percentile(values, 99),
    average: finiteValue(values?.avg),
    max: finiteValue(values?.max),
  };
}

function attemptsSummary(values, fallbackSamples) {
  return {
    ...(values || {}),
    samples: finiteValue(values?.count) ?? fallbackSamples,
    p50: percentile(values, 50),
    p95: percentile(values, 95),
    p99: percentile(values, 99),
    average: finiteValue(values?.avg),
    max: finiteValue(values?.max),
    raw: values,
  };
}

export function handleSummary(data) {
  const accepted = metricCount(data, BOOKINGS_ACCEPTED_METRIC);
  const soldOut = metricCount(data, BOOKINGS_SOLD_OUT_METRIC);
  const conflicts = metricCount(data, CONFLICT_EXHAUSTED_METRIC);
  const lockTimeout = metricCount(data, LOCK_TIMEOUT_METRIC);
  const unexpected = metricCount(data, UNEXPECTED_RESPONSES_METRIC);
  const submitted = metricCount(data, REQUESTS_SUBMITTED_METRIC);
  const timeoutCount = metricCount(data, TIMEOUTS_METRIC);
  const droppedValues = metricValues(data, 'dropped_iterations');
  const droppedCount = counterMetricCount(droppedValues);
  const acceptedLatencyValues = metricValues(data, ACCEPTED_LATENCY_METRIC);
  const terminalLatencyValues = metricValues(data, TERMINAL_LATENCY_METRIC);
  const requestStartValues = metricValues(data, REQUEST_START_ELAPSED_METRIC);
  const completionValues = metricValues(data, COMPLETION_ELAPSED_METRIC);
  const lastCompletionElapsedMs = finiteValue(completionValues?.max);
  const firstRequestStartElapsedMs = finiteValue(requestStartValues?.min);
  const measurementDurationMs = lastCompletionElapsedMs !== null
    && firstRequestStartElapsedMs !== null
    ? Math.max(0, lastCompletionElapsedMs - firstRequestStartElapsedMs)
    : null;
  const acceptedTps = measurementDurationMs !== null && measurementDurationMs > 0
    ? accepted / (measurementDurationMs / 1000)
    : null;
  const drainTimeMs = lastCompletionElapsedMs !== null
    ? Math.max(0, lastCompletionElapsedMs - config.durationSeconds * 1000)
    : null;
  const recognized = accepted + soldOut + conflicts + lockTimeout;
  const acceptedLatencySummary = latencySummary(acceptedLatencyValues, accepted);
  const timeoutValues = metricValues(data, REQUEST_TIMEOUT_METRIC);
  const timeoutSummary = {
    ...(timeoutValues || {}),
    count: timeoutCount,
    rate: finiteValue(timeoutValues?.rate),
    passes: finiteValue(timeoutValues?.passes),
    fails: finiteValue(timeoutValues?.fails),
    raw: timeoutValues,
  };
  const exactOutcomeCounts = accepted === expected.accepted
    && soldOut === expected.sold_out
    && conflicts === 0
    && lockTimeout === 0
    && unexpected === 0;
  const summaryOutput = handleSummaryWithMetadata(data, config, {
    strategy,
    phase,
    endpoint: `POST ${bookingPath}`,
    phase_case_count: cases.length,
    total_case_count: loadedCases.totalCases,
    expected_accepted: expected.accepted,
    expected_sold_out: expected.sold_out,
  });
  const summary = JSON.parse(summaryOutput[config.summaryFile]);
  summary.results = {
    strategy,
    phase,
    endpoint: `POST ${bookingPath}`,
    submitted,
    accepted,
    sold_out: soldOut,
    conflict_exhausted: conflicts,
    lock_timeout: lockTimeout,
    unexpected,
    exact_outcome_counts: exactOutcomeCounts,
    accepted_tps: acceptedTps,
    measurement_duration_ms: measurementDurationMs,
    drain_time_ms: drainTimeMs,
    accepted_latency_ms: acceptedLatencySummary,
    accepted_p50_ms: acceptedLatencySummary.p50,
    accepted_p95_ms: acceptedLatencySummary.p95,
    accepted_p99_ms: acceptedLatencySummary.p99,
    p50_ms: acceptedLatencySummary.p50,
    p95_ms: acceptedLatencySummary.p95,
    p99_ms: acceptedLatencySummary.p99,
    terminal_latency_ms: latencySummary(terminalLatencyValues, recognized),
    attempts: attemptsSummary(metricValues(data, ATTEMPT_COUNT_METRIC), submitted),
    timeouts: timeoutSummary,
    dropped: {
      ...(droppedValues || {}),
      count: droppedCount,
      raw: droppedValues,
    },
    database_invariant: 'verify_after_run',
  };
  return {
    [config.summaryFile]: JSON.stringify(summary, null, 2),
  };
}

export default function (runContext) {
  const index = exec.scenario.iterationInTest;
  if (index >= cases.length) {
    exec.test.abort(`Test data exhausted: index=${index}, size=${cases.length}`);
  }

  const testCase = cases[index];
  const { accessToken, phase: casePhase, ...request } = testCase;
  if (casePhase !== phase) {
    exec.test.abort(`Test data phase mismatch: expected=${phase}`);
  }

  const startedAt = Date.now();
  requestStartElapsed.add(startedAt - runContext.startedAt);
  const response = http.post(
    `${config.baseUrl}${bookingPath}`,
    JSON.stringify(request),
    {
      headers: {
        ...authorizationHeaders(accessToken),
        'Content-Type': 'application/json',
      },
      tags: {
        name: 'stock_contention_booking',
        phase,
        strategy,
      },
      timeout: config.requestTimeout,
    },
  );
  const result = parseBookingResult(response.body);
  const outcome = classifyBookingResponse({ status: response.status, body: response.body, phase });
  const outcomeTags = buildStockMetricTags({
    ...metricContext,
    outcome,
  });
  const timedOut = outcome === 'lock_timeout'
    || isTimeoutStatus(response.status)
    || isTimeoutResponse(response);
  const completedAt = Date.now();
  const elapsedMs = completedAt - runContext.startedAt;

  requestsSubmitted.add(1, buildStockMetricTags({ ...metricContext, outcome: 'submitted' }));
  bookingsAccepted.add(outcome === 'accepted' ? 1 : 0, outcomeTags);
  bookingsSoldOut.add(outcome === 'sold_out' ? 1 : 0, outcomeTags);
  conflictsExhausted.add(outcome === 'conflict_exhausted' ? 1 : 0, outcomeTags);
  lockTimeouts.add(outcome === 'lock_timeout' ? 1 : 0, outcomeTags);
  unexpectedResponses.add(outcome === 'unexpected' ? 1 : 0, outcomeTags);
  requestTimedOut.add(timedOut, outcomeTags);
  timeouts.add(timedOut ? 1 : 0, outcomeTags);
  attemptCount.add(result?.attemptCount ?? 0, outcomeTags);
  completionElapsed.add(elapsedMs);
  if (outcome !== 'unexpected') {
    terminalLatency.add(response.timings.duration);
  }
  if (outcome === 'accepted') {
    acceptedLatency.add(response.timings.duration);
  }

  check(
    response,
    { stock_contention_response_recognized: () => outcome !== 'unexpected' },
    {
      name: 'stock_contention_response_recognized',
      phase,
      strategy,
    },
  );
}
