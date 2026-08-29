import assert from 'node:assert/strict';
import test from 'node:test';
import { loadConfig } from '../lib/config.js';

const baseEnvironment = Object.freeze({
  BASE_URL: 'https://api-dev.beatlive.kr',
  TARGET_ENV: 'dev',
  TEST_ID: 'config-test',
  GIT_SHA: 'test-sha',
});

function load(overrides = {}, defaults = { scenario: 'generic_http' }) {
  return loadConfig({ ...baseEnvironment, ...overrides }, defaults);
}

test('target environment selects only its allowlisted hostname', () => {
  assert.equal(load().targetEnv, 'dev');
  assert.equal(load().serverType, 't4g.small');
  assert.equal(
    load({ TARGET_ENV: 'prod', BASE_URL: 'https://api.beatlive.kr' }).serverType,
    't4g.small',
  );
  assert.throws(
    () => load({ BASE_URL: 'https://api.beatlive.kr' }),
    /allowlisted/,
  );
});

test('load budget cannot be overridden by runtime arguments', () => {
  for (const name of ['TARGET_RPS', 'DURATION', 'PRE_ALLOCATED_VUS', 'MAX_VUS']) {
    assert.throws(
      () => load({ [name]: '2' }),
      /versioned LOAD_PROFILE budget/,
    );
  }
});

test('summary output accepts a basename and rejects path traversal', () => {
  assert.equal(load({ SUMMARY_FILE: 'summary.json' }).summaryFile, 'summary.json');
  for (const summaryFile of [
    '/tmp/summary.json',
    '../summary.json',
    'nested/summary.json',
    'nested\\summary.json',
  ]) {
    assert.throws(
      () => load({ SUMMARY_FILE: summaryFile }),
      /safe JSON basename/,
    );
  }
});

test('profile budgets keep the planned iteration counts fixed', () => {
  assert.deepEqual(
    {
      smoke: load().plannedIterations,
      baseline: load({ LOAD_PROFILE: 'baseline' }).plannedIterations,
      step: load({ LOAD_PROFILE: 'step' }).plannedIterations,
    },
    { smoke: 60, baseline: 1500, step: 1620 },
  );

  const ticketStep = load(
    { LOAD_PROFILE: 'step' },
    { scenario: 'ticket_confirmation' },
  );
  assert.equal(ticketStep.plannedIterations, 120);
  assert.equal(ticketStep.peakRps, 1);
});
