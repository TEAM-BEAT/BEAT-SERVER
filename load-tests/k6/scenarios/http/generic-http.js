import { check } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { Rate } from 'k6/metrics';

const requiredAcknowledgement = 'shared-rds-dev';
const absoluteMaxRps = 2;
const absoluteMaxDurationSeconds = 300;
const targetRps = Number(__ENV.TARGET_RPS || 1);
const duration = __ENV.DURATION || '1m';
const maxSafeRps = Number(__ENV.MAX_SAFE_RPS || 1);
const reuseRequests = __ENV.REUSE_REQUESTS === 'true';
const requestFailed = new Rate('api_request_failed');

function durationInSeconds(value) {
  const match = /^(\d+)(s|m)$/.exec(value);
  if (!match) {
    throw new Error('DURATION must use seconds or minutes, for example 30s or 2m.');
  }

  return Number(match[1]) * (match[2] === 'm' ? 60 : 1);
}

if (__ENV.LOAD_TEST_ACK !== requiredAcknowledgement) {
  throw new Error(`Set LOAD_TEST_ACK=${requiredAcknowledgement} after confirming the shared-RDS risk.`);
}
if (__ENV.TARGET_ENV !== 'dev') {
  throw new Error('TARGET_ENV must be dev.');
}
if (!__ENV.BASE_URL || !__ENV.ALLOWED_DEV_ORIGIN || !__ENV.PREFLIGHT_URL) {
  throw new Error('BASE_URL, ALLOWED_DEV_ORIGIN, and PREFLIGHT_URL are required.');
}

const baseUrl = __ENV.BASE_URL.replace(/\/+$/, '');
const allowedDevOrigin = __ENV.ALLOWED_DEV_ORIGIN.replace(/\/+$/, '');
const preflightUrl = __ENV.PREFLIGHT_URL.replace(/\/+$/, '');

if (!baseUrl.startsWith('https://') || baseUrl !== allowedDevOrigin) {
  throw new Error('BASE_URL must exactly match the approved HTTPS ALLOWED_DEV_ORIGIN.');
}
if (!preflightUrl.startsWith(`${baseUrl}/`)) {
  throw new Error('PREFLIGHT_URL must use the same approved dev origin as BASE_URL.');
}
if (!Number.isInteger(maxSafeRps) || maxSafeRps < 1 || maxSafeRps > absoluteMaxRps) {
  throw new Error(`MAX_SAFE_RPS must be between 1 and the absolute cap(${absoluteMaxRps}).`);
}
if (!Number.isInteger(targetRps) || targetRps < 1 || targetRps > maxSafeRps) {
  throw new Error(`TARGET_RPS must be between 1 and MAX_SAFE_RPS(${maxSafeRps}).`);
}

const durationSeconds = durationInSeconds(duration);
if (durationSeconds < 1 || durationSeconds > absoluteMaxDurationSeconds) {
  throw new Error(`DURATION must be between 1 and ${absoluteMaxDurationSeconds} seconds.`);
}
if (__ENV.MAX_P95_MS && (!Number.isFinite(Number(__ENV.MAX_P95_MS)) || Number(__ENV.MAX_P95_MS) <= 0)) {
  throw new Error('MAX_P95_MS must be a positive number.');
}

const requests = new SharedArray('generic-http-requests', () =>
  JSON.parse(open(__ENV.REQUEST_FILE || './requests.json')),
);
if (requests.length === 0) {
  throw new Error('REQUEST_FILE must contain at least one request.');
}

requests.forEach((request, index) => {
  if (!/^[a-z][a-z0-9_:-]{0,63}$/.test(request.name)) {
    throw new Error(`Invalid low-cardinality request name at index=${index}.`);
  }
  if (!/^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)$/i.test(request.method)) {
    throw new Error(`Invalid HTTP method at index=${index}.`);
  }
  if (typeof request.path !== 'string' || !request.path.startsWith('/') || request.path.startsWith('//')) {
    throw new Error(`Path must be origin-relative at index=${index}.`);
  }
  if (!Array.isArray(request.expectedStatuses) || request.expectedStatuses.length === 0) {
    throw new Error(`expectedStatuses is required at index=${index}.`);
  }
  if (!request.expectedStatuses.every((status) => Number.isInteger(status) && status >= 100 && status <= 599)) {
    throw new Error(`expectedStatuses contains an invalid HTTP status at index=${index}.`);
  }
  if (
    request.headers !== undefined
    && (request.headers === null || Array.isArray(request.headers) || typeof request.headers !== 'object')
  ) {
    throw new Error(`headers must be an object at index=${index}.`);
  }
});

const requiredRequests = targetRps * durationSeconds;
if (!reuseRequests && requests.length < requiredRequests) {
  throw new Error(
    `At least ${requiredRequests} requests are required for non-reuse mode, but only ${requests.length} were provided.`,
  );
}

const requestNames = [...new Set(requests.map((request) => request.name))];
const thresholds = {
  dropped_iterations: ['count==0'],
};
requestNames.forEach((name) => {
  thresholds[`api_request_failed{name:${name}}`] = ['rate<0.01'];
  if (__ENV.MAX_P95_MS) {
    thresholds[`http_req_duration{name:${name}}`] = [`p(95)<${Number(__ENV.MAX_P95_MS)}`];
  }
});

export const options = {
  discardResponseBodies: true,
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error_code'],
  scenarios: {
    generic_http: {
      executor: 'constant-arrival-rate',
      rate: targetRps,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 10),
      maxVUs: Number(__ENV.MAX_VUS || 50),
      tags: {
        workload: 'generic_http',
      },
    },
  },
  thresholds,
};

function authorizationHeaders() {
  return __ENV.ACCESS_TOKEN ? { Authorization: `Bearer ${__ENV.ACCESS_TOKEN}` } : {};
}

export function setup() {
  const response = http.get(preflightUrl, {
    headers: authorizationHeaders(),
    tags: { name: 'load_test_preflight' },
    timeout: '5s',
  });

  if (response.status !== Number(__ENV.PREFLIGHT_EXPECTED_STATUS || 200)) {
    throw new Error(`Load-test preflight failed with HTTP ${response.status}.`);
  }
}

export default function () {
  const index = exec.scenario.iterationInTest;
  if (!reuseRequests && index >= requests.length) {
    exec.test.abort(`Request data exhausted: index=${index}, size=${requests.length}`);
  }

  const request = requests[index % requests.length];
  const hasJsonBody = request.body !== undefined
    && request.body !== null
    && typeof request.body !== 'string';
  const headers = {
    ...(hasJsonBody ? { 'Content-Type': 'application/json' } : {}),
    ...authorizationHeaders(),
    ...(request.headers || {}),
  };
  const body = request.body === undefined || request.body === null
    ? null
    : typeof request.body === 'string'
      ? request.body
      : JSON.stringify(request.body);

  const response = http.request(
    request.method.toUpperCase(),
    `${baseUrl}${request.path}`,
    body,
    {
      headers,
      tags: { name: request.name },
      timeout: __ENV.REQUEST_TIMEOUT || '10s',
    },
  );
  const succeeded = request.expectedStatuses.includes(response.status);
  requestFailed.add(!succeeded, { name: request.name });

  check(
    response,
    {
      expected_status: () => succeeded,
    },
    { name: request.name },
  );
}
