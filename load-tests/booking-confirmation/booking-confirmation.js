import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';

const requiredAcknowledgement = 'shared-rds-dev';
const absoluteMaxRps = 2;
const absoluteMaxDurationSeconds = 300;
const targetRps = Number(__ENV.TARGET_RPS || 1);
const duration = __ENV.DURATION || '1m';
const expectedItemCount = Number(__ENV.ITEM_COUNT || 78);
const maxSafeRps = Number(__ENV.MAX_SAFE_RPS || 1);

function durationInSeconds(value) {
  const match = /^(\d+)(s|m|h)$/.exec(value);
  if (!match) {
    return Number.NaN;
  }

  const multiplier = { s: 1, m: 60, h: 3600 }[match[2]];
  return Number(match[1]) * multiplier;
}

if (__ENV.LOAD_TEST_ACK !== requiredAcknowledgement) {
  throw new Error(`Set LOAD_TEST_ACK=${requiredAcknowledgement} after confirming the shared-RDS risk.`);
}
if (!__ENV.BASE_URL || !__ENV.ACCESS_TOKEN) {
  throw new Error('BASE_URL and ACCESS_TOKEN are required.');
}
if (!__ENV.PREFLIGHT_URL) {
  throw new Error('PREFLIGHT_URL is required.');
}
if (__ENV.TARGET_ENV !== 'dev') {
  throw new Error('TARGET_ENV=dev is required.');
}
if (!__ENV.ALLOWED_DEV_ORIGIN) {
  throw new Error('ALLOWED_DEV_ORIGIN is required.');
}
const baseUrl = __ENV.BASE_URL.replace(/\/+$/, '');
const allowedDevOrigin = __ENV.ALLOWED_DEV_ORIGIN.replace(/\/+$/, '');
if (!baseUrl.startsWith('https://') || baseUrl !== allowedDevOrigin) {
  throw new Error('BASE_URL must exactly match the approved HTTPS ALLOWED_DEV_ORIGIN.');
}
const preflightUrl = __ENV.PREFLIGHT_URL.replace(/\/+$/, '');
if (!preflightUrl.startsWith(`${baseUrl}/`)) {
  throw new Error('PREFLIGHT_URL must use the same approved dev origin as BASE_URL.');
}
if (!Number.isInteger(maxSafeRps) || maxSafeRps < 1 || maxSafeRps > absoluteMaxRps) {
  throw new Error(`MAX_SAFE_RPS must be between 1 and the absolute cap(${absoluteMaxRps}).`);
}
if (!Number.isInteger(targetRps) || targetRps < 1 || targetRps > maxSafeRps) {
  throw new Error(`TARGET_RPS must be an integer between 1 and MAX_SAFE_RPS(${maxSafeRps}).`);
}
if (!Number.isInteger(expectedItemCount) || expectedItemCount < 1) {
  throw new Error('ITEM_COUNT must be a positive integer.');
}
if (durationInSeconds(duration) > absoluteMaxDurationSeconds || Number.isNaN(durationInSeconds(duration))) {
  throw new Error(`DURATION must not exceed the absolute cap(${absoluteMaxDurationSeconds}s).`);
}
if (__ENV.MAX_P95_MS && (!Number.isFinite(Number(__ENV.MAX_P95_MS)) || Number(__ENV.MAX_P95_MS) <= 0)) {
  throw new Error('MAX_P95_MS must be a positive number.');
}

const cases = new SharedArray('booking-confirmation-cases', () =>
  JSON.parse(open(__ENV.DATA_FILE || './cases.json')),
);

const requiredCases = targetRps * durationInSeconds(duration);
if (cases.length < requiredCases) {
  throw new Error(`At least ${requiredCases} unique cases are required, but only ${cases.length} were provided.`);
}

const bookingIds = new Set();
cases.forEach((request, requestIndex) => {
  if (!Number.isInteger(request.performanceId) || request.performanceId < 1) {
    throw new Error(`Invalid performanceId at request index=${requestIndex}.`);
  }
  if (!Array.isArray(request.bookingList) || request.bookingList.length !== expectedItemCount) {
    throw new Error(
      `Invalid item count at request index=${requestIndex}: expected=${expectedItemCount}, actual=${request.bookingList?.length}`,
    );
  }

  request.bookingList.forEach((booking, bookingIndex) => {
    if (!Number.isInteger(booking.bookingId) || booking.bookingId < 1) {
      throw new Error(`Invalid bookingId at request=${requestIndex}, booking=${bookingIndex}.`);
    }
    if (booking.bookingStatus !== 'BOOKING_CONFIRMED') {
      throw new Error(`bookingStatus must be BOOKING_CONFIRMED at bookingId=${booking.bookingId}.`);
    }
    if (bookingIds.has(booking.bookingId)) {
      throw new Error(`Duplicate bookingId=${booking.bookingId} across the data set.`);
    }
    bookingIds.add(booking.bookingId);
  });
});

const requestedItems = new Counter('booking_items_requested');
const succeededItems = new Counter('booking_items_succeeded');

const thresholds = {
  'http_req_failed{name:ticket_update}': ['rate<0.01'],
  'checks{name:ticket_update_status}': ['rate>0.99'],
  dropped_iterations: ['count==0'],
};

if (__ENV.MAX_P95_MS) {
  thresholds['http_req_duration{name:ticket_update}'] = [`p(95)<${Number(__ENV.MAX_P95_MS)}`];
}

export const options = {
  discardResponseBodies: true,
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error_code'],
  scenarios: {
    booking_confirmation: {
      executor: 'constant-arrival-rate',
      rate: targetRps,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 50),
      gracefulStop: '30s',
      tags: {
        workload: 'booking_confirmation',
      },
    },
  },
  thresholds,
};

export function setup() {
  const response = http.get(preflightUrl, {
    headers: {
      Authorization: `Bearer ${__ENV.ACCESS_TOKEN}`,
    },
    responseType: 'text',
    tags: {
      name: 'load_test_preflight',
    },
    timeout: '5s',
  });

  if (response.status !== 200) {
    throw new Error(`Load-test preflight failed with HTTP ${response.status}.`);
  }

  const marker = response.json('loadTest');
  if (marker?.enabled !== true || marker?.ticketConfirmationSmsEnabled !== false) {
    throw new Error('Server is not load-test ready or real confirmation SMS is enabled.');
  }
}

export default function () {
  const index = exec.scenario.iterationInTest;
  if (index >= cases.length) {
    exec.test.abort(`Test data exhausted: index=${index}, size=${cases.length}`);
  }

  const request = cases[index];
  if (!Array.isArray(request.bookingList) || request.bookingList.length !== expectedItemCount) {
    exec.test.abort(
      `Invalid item count at index=${index}: expected=${expectedItemCount}, actual=${request.bookingList?.length}`,
    );
  }

  requestedItems.add(request.bookingList.length);

  const response = http.put(`${baseUrl}/api/tickets/update`, JSON.stringify(request), {
    headers: {
      Authorization: `Bearer ${__ENV.ACCESS_TOKEN}`,
      'Content-Type': 'application/json',
    },
    tags: {
      name: 'ticket_update',
    },
    timeout: __ENV.REQUEST_TIMEOUT || '30s',
  });

  const success = check(
    response,
    {
      'ticket update status is 200': (result) => result.status === 200,
    },
    {
      name: 'ticket_update_status',
    },
  );

  if (success) {
    succeededItems.add(request.bookingList.length);
  }
}
