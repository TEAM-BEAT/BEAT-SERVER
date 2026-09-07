import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { loadConfig } from '../lib/config.js';
import { handleSummaryWithMetadata } from '../lib/summary.js';
import {
  assertDevOnlyTarget,
  assertStockContentionTimeout,
  buildStockContentionRequest,
  buildStockMetricTags,
  classifyBookingResponse,
  counterMetricCount,
  exactOutcomeThresholds,
  parseBookingResult,
  STOCK_CONTENTION_CASE_COUNTS,
  STOCK_CONTENTION_EXPERIMENT_PATH,
  STOCK_CONTENTION_REQUEST_TIMEOUT,
  STOCK_CONTENTION_QUIET_PERIOD_SECONDS,
  STOCK_CONTENTION_REPETITIONS,
  STOCK_CONTENTION_SCHEMA_VERSION,
  STOCK_CONTENTION_STRATEGIES,
  STOCK_CONTENTION_STRATEGY_ROTATIONS,
  STOCK_CONTENTION_TOTAL_CASE_COUNT,
  stockContentionBookingPath,
  validateStockCases,
  validateStrategy,
} from '../scenarios/stock-contention/contract.js';

const stockContentionScenarioSource = readFileSync(
  new URL('../scenarios/stock-contention/stock-contention.js', import.meta.url),
  'utf8',
);
const stockContentionCasesSource = readFileSync(
  new URL('../scenarios/stock-contention/cases.js', import.meta.url),
  'utf8',
);

const baseEnvironment = Object.freeze({
  BASE_URL: 'https://api-dev.beatlive.kr',
  TARGET_ENV: 'dev',
  TEST_ID: 'stock-contention-test',
  GIT_SHA: 'test-sha',
});

function load(profile, overrides = {}) {
  return loadConfig(
    { ...baseEnvironment, LOAD_PROFILE: profile, ...overrides },
    {
      scenario: 'stock_contention',
      requestTimeout: STOCK_CONTENTION_REQUEST_TIMEOUT,
      includeProfileInSummaryFile: true,
    },
  );
}

function validDataset() {
  return {
    schema_version: 'v3',
    accessToken: 'test-token-shared',
    warmupScheduleId: 9001,
    flashScheduleId: 9002,
    bookerName: 'LoadTest',
    bookerPhoneNumber: '010-0000-0001',
  };
}

test('stock contention target accepts only dev', () => {
  assert.doesNotThrow(() => assertDevOnlyTarget('dev'));
  assert.throws(() => assertDevOnlyTarget('prod'), /dev-only/);
});

test('stock contention dataset keeps the fixed iteration counts and exact six-field config', () => {
  const result = validateStockCases(validDataset());

  assert.equal(STOCK_CONTENTION_SCHEMA_VERSION, 'v3');
  assert.deepEqual(Object.keys(validDataset()).sort(), [
    'accessToken',
    'bookerName',
    'bookerPhoneNumber',
    'flashScheduleId',
    'schema_version',
    'warmupScheduleId',
  ]);
  assert.equal(STOCK_CONTENTION_CASE_COUNTS.warmup, 900);
  assert.equal(STOCK_CONTENTION_CASE_COUNTS.flash, 200);
  assert.equal(result.totalCases, STOCK_CONTENTION_TOTAL_CASE_COUNT);
  assert.equal(result.accessToken, 'test-token-shared');
  assert.equal(result.warmupScheduleId, 9001);
  assert.equal(result.flashScheduleId, 9002);
});

test('stock contention dataset rejects case-level and unknown token fields', () => {
  for (const unknownField of ['cases', 'memberAccessToken', 'purchaseTicketCount']) {
    const unknownDataset = validDataset();
    unknownDataset[unknownField] = unknownField;
    assert.throws(() => validateStockCases(unknownDataset), /unsupported field/);
  }
});

test('stock contention dataset requires one valid top-level accessToken', () => {
  const absentDataset = validDataset();
  delete absentDataset.accessToken;
  assert.throws(() => validateStockCases(absentDataset), /non-empty accessToken/);

  for (const invalidToken of ['', ' token', 'token ', 'to ken', '\n']) {
    const invalidDataset = validDataset();
    invalidDataset.accessToken = invalidToken;
    assert.throws(() => validateStockCases(invalidDataset), /non-empty accessToken/);
  }
});

test('stock contention dataset rejects invalid schedule IDs and contact fields', () => {
  for (const field of ['warmupScheduleId', 'flashScheduleId']) {
    for (const invalidValue of [0, -1, 1.5, '9001', null]) {
      const invalidDataset = validDataset();
      invalidDataset[field] = invalidValue;
      assert.throws(() => validateStockCases(invalidDataset), new RegExp(`invalid ${field}`));
    }
  }

  for (const invalidName of ['', 'Load Test', 'LoadTest1']) {
    const invalidDataset = validDataset();
    invalidDataset.bookerName = invalidName;
    assert.throws(() => validateStockCases(invalidDataset), /invalid bookerName/);
  }
  for (const invalidPhone of ['', '010-0000-000', '010 0000 0001']) {
    const invalidDataset = validDataset();
    invalidDataset.bookerPhoneNumber = invalidPhone;
    assert.throws(() => validateStockCases(invalidDataset), /invalid bookerPhoneNumber/);
  }
});

test('stock contention sends the validated shared token through loadCases to Authorization', () => {
  assert.match(stockContentionCasesSource, /const validated = validateStockCases\(dataset\);/);
  assert.match(stockContentionCasesSource, /accessToken:\s*validated\.accessToken/);
  assert.match(stockContentionCasesSource, /request:\s*buildStockContentionRequest\(validated,\s*config\.profile\)/);
  assert.match(stockContentionScenarioSource, /const \{ accessToken, request, phaseCaseCount \} = loadedCases;/);
  assert.match(stockContentionScenarioSource, /\.\.\.authorizationHeaders\(accessToken\)/);
  assert.doesNotMatch(stockContentionScenarioSource, /const \{ accessToken,\s*phase:/);
});

test('stock contention dataset keeps one schedule per phase and requires one ticket', () => {
  const sameScheduleDataset = validDataset();
  sameScheduleDataset.flashScheduleId = sameScheduleDataset.warmupScheduleId;
  assert.throws(() => validateStockCases(sameScheduleDataset), /scheduleId values must be different/);

  const validated = validateStockCases(validDataset());
  assert.deepEqual(buildStockContentionRequest(validated, 'warmup'), {
    scheduleId: 9001,
    purchaseTicketCount: 1,
    bookerName: 'LoadTest',
    bookerPhoneNumber: '010-0000-0001',
  });
  assert.deepEqual(buildStockContentionRequest(validated, 'flash'), {
    scheduleId: 9002,
    purchaseTicketCount: 1,
    bookerName: 'LoadTest',
    bookerPhoneNumber: '010-0000-0001',
  });
  assert.throws(
    () => buildStockContentionRequest(validated, 'invalid'),
    /Invalid stock contention phase/,
  );
});

test('stock contention classifier parses every experiment outcome and attempt count', () => {
  assert.equal(
    classifyBookingResponse({
      status: 201,
      body: JSON.stringify({ outcome: 'ACCEPTED', bookingId: 1, attemptCount: 1 }),
      phase: 'warmup',
    }),
    'accepted',
  );
  assert.equal(
    classifyBookingResponse({
      status: 200,
      body: JSON.stringify({ outcome: 'SOLD_OUT', bookingId: null, attemptCount: 1 }),
      phase: 'flash',
    }),
    'sold_out',
  );
  assert.equal(
    classifyBookingResponse({
      status: 200,
      body: JSON.stringify({ outcome: 'SOLD_OUT', bookingId: null, attemptCount: 1 }),
      phase: 'warmup',
    }),
    'unexpected',
  );
  assert.equal(
    classifyBookingResponse({ status: 400, body: JSON.stringify({ status: 400 }), phase: 'flash' }),
    'unexpected',
  );
  assert.equal(
    classifyBookingResponse({
      status: 200,
      body: JSON.stringify({ outcome: 'CONFLICT_EXHAUSTED', bookingId: null, attemptCount: 50 }),
      phase: 'flash',
    }),
    'conflict_exhausted',
  );
  assert.equal(
    classifyBookingResponse({
      status: 200,
      body: JSON.stringify({ outcome: 'LOCK_TIMEOUT', bookingId: null, attemptCount: 0 }),
      phase: 'flash',
    }),
    'lock_timeout',
  );
  assert.equal(
    classifyBookingResponse({ status: 500, body: 'not-json', phase: 'flash' }),
    'unexpected',
  );
  assert.deepEqual(
    parseBookingResult(JSON.stringify({ outcome: 'LOCK_TIMEOUT', bookingId: null, attemptCount: 0 })),
    { outcome: 'LOCK_TIMEOUT', bookingId: null, attemptCount: 0 },
  );
  assert.equal(
    parseBookingResult(JSON.stringify({ outcome: 'ACCEPTED', bookingId: 1, attemptCount: 3 })).attemptCount,
    3,
  );
  assert.equal(parseBookingResult(JSON.stringify({ outcome: 'UNKNOWN', bookingId: null, attemptCount: 1 })), null);
  assert.equal(parseBookingResult(JSON.stringify({ outcome: 'ACCEPTED', bookingId: '1', attemptCount: 1 })), null);
  assert.equal(parseBookingResult(JSON.stringify({ outcome: 'ACCEPTED', bookingId: 1, attemptCount: 0 })), null);
  assert.equal(parseBookingResult(JSON.stringify({ outcome: 'ACCEPTED', bookingId: 1 })), null);
});

test('stock contention metric tags contain only low-cardinality fields', () => {
  const tags = buildStockMetricTags({
    testId: 'stock-contention-test',
    gitSha: 'test-sha',
    strategy: 'ATOMIC',
    phase: 'flash',
    outcome: 'sold_out',
  });

  assert.deepEqual(tags, {
    test_id: 'stock-contention-test',
    git_sha: 'test-sha',
    strategy: 'ATOMIC',
    phase: 'flash',
    outcome: 'sold_out',
  });
  for (const forbidden of [
    'accessToken',
    'member_id',
    'schedule_id',
    'booking_id',
    'phone_number',
  ]) {
    assert.equal(Object.hasOwn(tags, forbidden), false);
  }
  assert.throws(
    () => buildStockMetricTags({
      testId: 'stock-contention-test',
      gitSha: 'test-sha',
      strategy: 'ATOMIC',
      phase: 'flash',
      outcome: 'schedule-9002',
    }),
    /Invalid stock contention metric outcome/,
  );
});

test('stock contention never exposes the shared token in artifacts or validation errors', () => {
  const sharedToken = 'do-not-emit-shared-token';
  const tags = buildStockMetricTags({
    testId: 'stock-contention-test',
    gitSha: 'test-sha',
    strategy: 'ATOMIC',
    phase: 'flash',
    outcome: 'accepted',
  });
  assert.equal(JSON.stringify(tags).includes(sharedToken), false);

  const output = handleSummaryWithMetadata(
    { metrics: {}, root_group: {} },
    {
      testId: 'stock-contention-test',
      gitSha: 'test-sha',
      scenario: 'stock_contention',
      datasetHash: 'a'.repeat(64),
      targetEnv: 'dev',
      serverType: 't4g.small',
      profile: 'flash',
      budgetVersion: 'v1',
      duration: '1s',
      peakRps: 200,
      plannedIterations: 200,
      summaryFile: 'summary.json',
      accessToken: sharedToken,
    },
  );
  assert.equal(output['summary.json'].includes(sharedToken), false);

  const invalidDataset = validDataset();
  invalidDataset.accessToken = ` ${sharedToken}`;
  assert.throws(
    () => validateStockCases(invalidDataset),
    (error) => error.message.includes('non-empty accessToken')
      && !error.message.includes(sharedToken),
  );
});

test('stock contention summary counts Counter values without treating Rate samples as unexpected', () => {
  assert.equal(counterMetricCount({ count: 3, passes: 3, fails: 97 }), 3);
  assert.equal(counterMetricCount({ passes: 3, fails: 97 }), 0);
  assert.equal(counterMetricCount(undefined), 0);
});

test('stock contention outcome thresholds enforce exact warmup and flash semantics', () => {
  assert.deepEqual(exactOutcomeThresholds('warmup'), {
    accepted: 'count==900',
    sold_out: 'count==0',
    unexpected: 'count==0',
  });
  assert.deepEqual(exactOutcomeThresholds('flash'), {
    accepted: 'count==100',
    sold_out: 'count==100',
    unexpected: 'count==0',
  });
});

test('stock contention profiles keep warmup and flash budgets versioned', () => {
  const warmup = load('warmup');
  assert.equal(warmup.targetRps, 5);
  assert.equal(warmup.durationSeconds, 180);
  assert.equal(warmup.plannedIterations, 900);
  assert.equal(warmup.summaryFile, 'summary-stock-contention-test-warmup.json');

  const flash = load('flash');
  assert.equal(flash.targetRps, 200);
  assert.equal(flash.durationSeconds, 1);
  assert.equal(flash.plannedIterations, 200);
  assert.equal(flash.summaryFile, 'summary-stock-contention-test-flash.json');
  assert.equal(flash.budget.hardCap.maxRps, 200);
  assert.equal(flash.budget.hardCap.maxDurationSeconds, 180);
  assert.equal(flash.requestTimeout, STOCK_CONTENTION_REQUEST_TIMEOUT);
  assert.equal(warmup.requestTimeout, STOCK_CONTENTION_REQUEST_TIMEOUT);
  assert.equal(warmup.gracefulStop, '35s');
  assert.equal(flash.gracefulStop, '35s');

  for (const name of ['TARGET_RPS', 'DURATION', 'PRE_ALLOCATED_VUS', 'MAX_VUS', 'GRACEFUL_STOP']) {
    assert.throws(
      () => load('flash', { [name]: '1' }),
      /versioned LOAD_PROFILE budget/,
    );
  }
});

test('stock contention request timeout cannot drift from the 35-second contract', () => {
  assert.equal(assertStockContentionTimeout(STOCK_CONTENTION_REQUEST_TIMEOUT), '35s');
  assert.throws(() => assertStockContentionTimeout('30s'), /fixed to 35s/);
});

test('stock contention booking request does not follow redirects', () => {
  assert.match(stockContentionScenarioSource, /redirects:\s*0/);
});

test('stock contention strategy is restricted to the four comparison strategies', () => {
  assert.equal(validateStrategy('ATOMIC'), 'ATOMIC');
  assert.throws(() => validateStrategy('atomic_update'), /STRATEGY must be/);
});

test('stock contention rotation covers each strategy once in every repetition block', () => {
  assert.equal(STOCK_CONTENTION_REPETITIONS, 5);
  assert.equal(STOCK_CONTENTION_QUIET_PERIOD_SECONDS, 60);
  assert.equal(STOCK_CONTENTION_STRATEGY_ROTATIONS.length, STOCK_CONTENTION_REPETITIONS);
  for (const rotation of STOCK_CONTENTION_STRATEGY_ROTATIONS) {
    assert.deepEqual([...new Set(rotation)].sort(), [...STOCK_CONTENTION_STRATEGIES].sort());
  }
});

test('stock contention strategy selects the dev experiment endpoint without changing the base URL', () => {
  assert.equal(
    stockContentionBookingPath('PESSIMISTIC'),
    `${STOCK_CONTENTION_EXPERIMENT_PATH}/PESSIMISTIC/bookings`,
  );
  assert.equal(
    stockContentionBookingPath('OPTIMISTIC'),
    `${STOCK_CONTENTION_EXPERIMENT_PATH}/OPTIMISTIC/bookings`,
  );
  assert.equal(
    stockContentionBookingPath('REDIS'),
    `${STOCK_CONTENTION_EXPERIMENT_PATH}/REDIS/bookings`,
  );
  assert.equal(
    stockContentionBookingPath('ATOMIC'),
    `${STOCK_CONTENTION_EXPERIMENT_PATH}/ATOMIC/bookings`,
  );
  assert.throws(() => stockContentionBookingPath('prod'), /STRATEGY must be/);
});

test('stock contention summary metadata keeps strategy and endpoint in the JSON artifact', () => {
  const output = handleSummaryWithMetadata(
    { metrics: {}, root_group: {} },
    {
      testId: 'stock-contention-test',
      gitSha: 'test-sha',
      scenario: 'stock_contention',
      datasetHash: 'a'.repeat(64),
      targetEnv: 'dev',
      serverType: 't4g.small',
      profile: 'flash',
      budgetVersion: 'v1',
      duration: '1s',
      peakRps: 200,
      plannedIterations: 200,
      summaryFile: 'summary.json',
    },
    {
      strategy: 'ATOMIC',
      phase: 'flash',
      endpoint: 'POST /api/internal/experiments/stock-contention/ATOMIC/bookings',
    },
  );
  const summary = JSON.parse(output['summary.json']);
  assert.equal(summary.metadata.strategy, 'ATOMIC');
  assert.equal(summary.metadata.endpoint, 'POST /api/internal/experiments/stock-contention/ATOMIC/bookings');
});
