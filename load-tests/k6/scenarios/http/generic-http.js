import { check } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';
import { Rate } from 'k6/metrics';
import {
  addOptionalP95Threshold,
  constantArrivalRateScenario,
  loadSafeProdConfig,
} from '../../lib/config.js';
import { assertPreflight, authorizationHeaders } from '../../lib/http.js';
import { loadRequests } from './requests.js';

const config = loadSafeProdConfig(__ENV);
const requestFailed = new Rate('api_request_failed');
const { requests, reuseRequests } = loadRequests(__ENV, config);

const thresholds = { dropped_iterations: ['count==0'] };
[...new Set(requests.map((request) => request.name))].forEach((name) => {
  thresholds[`api_request_failed{name:${name}}`] = ['rate<0.01'];
  addOptionalP95Threshold(thresholds, `http_req_duration{name:${name}}`, config.maxP95Ms);
});

export const options = {
  discardResponseBodies: true,
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error_code'],
  scenarios: {
    generic_http: constantArrivalRateScenario('generic_http', config),
  },
  thresholds,
};

export function setup() {
  assertPreflight(config);
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
    ...authorizationHeaders(config.accessToken),
    ...(request.headers || {}),
  };
  const body = request.body === undefined || request.body === null
    ? null
    : typeof request.body === 'string'
      ? request.body
      : JSON.stringify(request.body);

  const response = http.request(request.method.toUpperCase(), `${config.baseUrl}${request.path}`, body, {
    headers,
    tags: { name: request.name },
    timeout: config.requestTimeout,
  });
  const succeeded = request.expectedStatuses.includes(response.status);
  requestFailed.add(!succeeded, { name: request.name });
  check(response, { expected_status: () => succeeded }, { name: request.name });
}
