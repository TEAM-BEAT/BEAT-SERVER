import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { loadConfig } from '../lib/config.js';
import { handleSummaryWithMetadata } from '../lib/summary.js';
import {
  assertDevOnlyTarget,
  assertStockContentionTimeout,
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
  STOCK_CONTENTION_STRATEGIES,
  STOCK_CONTENTION_STRATEGY_ROTATIONS,
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

function makeCase(phase, index, scheduleId) {
  const phoneGroup = phase === 'warmup' ? '0000' : '0001';
  return {
    phase,
    scheduleId,
    purchaseTicketCount: 1,
    bookerName: 'LoadTest',
    bookerPhoneNumber: `010-${phoneGroup}-${String(index + 1).padStart(4, '0')}`,
  };
}

function validDataset() {
  return {
    schema_version: 'v2',
    accessToken: 'test-token-shared',
    cases: [
      ...Array.from(
        { length: STOCK_CONTENTION_CASE_COUNTS.warmup },
        (_, index) => makeCase('warmup', index, 9001),
      ),
      ...Array.from(
        { length: STOCK_CONTENTION_CASE_COUNTS.flash },
        (_, index) => makeCase('flash', index, 9002),
      ),
    ],
  };
}

test('stock contention target accepts only dev', () => {
  assert.doesNotThrow(() => assertDevOnlyTarget('dev'));
  assert.throws(() => assertDevOnlyTarget('prod'), /dev-only/);
});

test('stock contention dataset keeps the exact phase counts and one shared token', () => {
  const result = validateStockCases(validDataset());

  assert.equal(result.warmup.length, 900);
  assert.equal(result.flash.length, 200);
  assert.equal(result.totalCases, 1100);
  assert.equal(result.accessToken, 'test-token-shared');
  assert.equal(Object.hasOwn(result.warmup[0], 'accessToken'), false);
  assert.equal(Object.hasOwn(result.flash[0], 'accessToken'), false);
  assert.notEqual(result.warmup[0].scheduleId, result.flash[0].scheduleId);
});

test('stock contention dataset rejects case-level and unknown token fields', () => {
  const caseTokenDataset = validDataset();
  caseTokenDataset.cases[0].accessToken = 'case-token';
  assert.throws(
    () => validateStockCases(caseTokenDataset),
    /unsupported field/,
  );

  const unknownTokenDataset = validDataset();
  unknownTokenDataset.memberAccessToken = 'unknown-token';
  assert.throws(() => validateStockCases(unknownTokenDataset), /unsupported field/);
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

test('stock contention sends the validated shared token through loadCases to Authorization', () => {
  assert.match(stockContentionCasesSource, /const validated = validateStockCases\(dataset\);/);
  assert.match(stockContentionCasesSource, /accessToken:\s*validated\.accessToken/);
  assert.match(stockContentionScenarioSource, /const \{ accessToken, cases \} = loadedCases;/);
  assert.match(stockContentionScenarioSource, /\.\.\.authorizationHeaders\(accessToken\)/);
  assert.doesNotMatch(stockContentionScenarioSource, /const \{ accessToken,\s*phase:/);
});

test('stock contention dataset keeps one schedule per phase and requires one ticket', () => {
  const mixedScheduleDataset = validDataset();
  mixedScheduleDataset.cases[1].scheduleId = 9003;
  assert.throws(() => validateStockCases(mixedScheduleDataset), /one scheduleId/);

  const sameScheduleDataset = validDataset();
  for (const stockCase of sameScheduleDataset.cases.slice(900)) {
    stockCase.scheduleId = 9001;
  }
  assert.throws(() => validateStockCases(sameScheduleDataset), /different scheduleId/);

  const multipleTicketDataset = validDataset();
  multipleTicketDataset.cases[900].purchaseTicketCount = 2;
  assert.throws(() => validateStockCases(multipleTicketDataset), /exactly one ticket/);
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
      endpoint: 'POST /internal/experiments/stock-contention/ATOMIC/bookings',
    },
  );
  const summary = JSON.parse(output['summary.json']);
  assert.equal(summary.metadata.strategy, 'ATOMIC');
  assert.equal(summary.metadata.endpoint, 'POST /internal/experiments/stock-contention/ATOMIC/bookings');
});
