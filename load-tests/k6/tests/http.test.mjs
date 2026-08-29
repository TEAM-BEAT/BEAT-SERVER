import assert from 'node:assert/strict';
import test from 'node:test';
import { isTimeoutResponse } from '../lib/timeout.js';

test('k6 request timeout error codes are detected', () => {
  assert.equal(isTimeoutResponse({ error_code: 1050 }), true);
  assert.equal(isTimeoutResponse({ error_code: 1211 }), true);
  assert.equal(isTimeoutResponse({ error_code: '1211' }), true);
});

test('k6 timeout messages without a numeric code are detected', () => {
  assert.equal(isTimeoutResponse({ error: 'dial tcp: i/o timeout' }), true);
  assert.equal(isTimeoutResponse({ error: 'context deadline exceeded' }), true);
  assert.equal(isTimeoutResponse({ error: 'connection refused' }), false);
  assert.equal(isTimeoutResponse({ status: 504 }), false);
});
