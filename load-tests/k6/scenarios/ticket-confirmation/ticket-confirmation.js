import { check } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';
import { Counter, Rate } from 'k6/metrics';
import {
  addOptionalP95Threshold,
  constantArrivalRateScenario,
  loadSafeProdConfig,
} from '../../lib/config.js';
import { assertPreflight, authorizationHeaders } from '../../lib/http.js';
import { loadCases } from './cases.js';

const TICKET_CONFIRMATION_ACKNOWLEDGEMENT = 'synthetic-confirmation-data-ready';
const config = loadSafeProdConfig(__ENV, { preAllocatedVUs: 20, requestTimeout: '30s' });
if (!config.accessToken) {
  throw new Error('ACCESS_TOKEN is required.');
}
if (__ENV.TICKET_CONFIRMATION_TEST_ACK !== TICKET_CONFIRMATION_ACKNOWLEDGEMENT) {
  throw new Error(
    `Set TICKET_CONFIRMATION_TEST_ACK=${TICKET_CONFIRMATION_ACKNOWLEDGEMENT} after preparing isolated synthetic data.`,
  );
}

const cases = loadCases(__ENV, config);

const itemsSubmitted = new Counter('ticket_confirmation_items_submitted');
const itemsAccepted = new Counter('ticket_confirmation_items_accepted');
const requestFailed = new Rate('ticket_confirmation_request_failed');
const thresholds = addOptionalP95Threshold(
  {
    ticket_confirmation_request_failed: ['rate<0.01'],
    'checks{name:ticket_confirmation_accepted}': ['rate>0.99'],
    dropped_iterations: ['count==0'],
  },
  'http_req_duration{name:ticket_confirmation_update}',
  config.maxP95Ms,
);

export const options = {
  discardResponseBodies: true,
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error_code'],
  scenarios: {
    ticket_confirmation: constantArrivalRateScenario(
      'ticket_confirmation',
      config,
    ),
  },
  thresholds,
};

export function setup() {
  assertPreflight(config);
}

export default function () {
  const index = exec.scenario.iterationInTest;
  if (index >= cases.length) {
    exec.test.abort(`Test data exhausted: index=${index}, size=${cases.length}`);
  }

  const request = cases[index];
  itemsSubmitted.add(request.bookingList.length);

  const response = http.put(`${config.baseUrl}/api/tickets/update`, JSON.stringify(request), {
    headers: {
      ...authorizationHeaders(config.accessToken),
      'Content-Type': 'application/json',
    },
    tags: { name: 'ticket_confirmation_update' },
    timeout: config.requestTimeout,
  });
  const accepted = response.status === 200;
  requestFailed.add(!accepted);
  check(
    response,
    { 'ticket confirmation accepted': () => accepted },
    { name: 'ticket_confirmation_accepted' },
  );

  if (accepted) {
    itemsAccepted.add(request.bookingList.length);
  }
}
