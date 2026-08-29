import { check } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';
import { Counter, Rate } from 'k6/metrics';
import {
  addAbortableRateThreshold,
  addDroppedIterationsThreshold,
  addOptionalP95Threshold,
  constantArrivalRateScenario,
  loadConfig,
  withDatasetHash,
} from '../../lib/config.js';
import {
  assertPreflight,
  authorizationHeaders,
  isTimeoutResponse,
  warnIfProduction,
} from '../../lib/http.js';
import { handleSummaryWithMetadata } from '../../lib/summary.js';
import { loadCases } from './cases.js';

const initialConfig = loadConfig(__ENV, {
  scenario: 'ticket_confirmation',
  requestTimeout: '30s',
});
if (!initialConfig.accessToken) {
  throw new Error('ACCESS_TOKEN is required.');
}

const loadedCases = loadCases(__ENV, initialConfig);
const { cases } = loadedCases;
const config = withDatasetHash(initialConfig, loadedCases.datasetHash);

const itemsSubmitted = new Counter('ticket_confirmation_items_submitted');
const itemsAccepted = new Counter('ticket_confirmation_items_accepted');
const requestFailed = new Rate('ticket_confirmation_request_failed');
const requestTimedOut = new Rate('ticket_confirmation_request_timeout');
const thresholds = {
  'checks{name:ticket_confirmation_accepted}': ['rate>0.99'],
};
addAbortableRateThreshold(thresholds, 'ticket_confirmation_request_failed');
addAbortableRateThreshold(thresholds, 'ticket_confirmation_request_timeout');
addDroppedIterationsThreshold(thresholds);
addOptionalP95Threshold(
  thresholds,
  'http_req_duration{name:ticket_confirmation_update}',
  config.maxP95Ms,
);

export const options = {
  discardResponseBodies: true,
  tags: config.tags,
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
  warnIfProduction(config);
  assertPreflight(config);
}

export function handleSummary(data) {
  return handleSummaryWithMetadata(data, config);
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
  requestTimedOut.add(isTimeoutResponse(response));
  check(
    response,
    { 'ticket confirmation accepted': () => accepted },
    { name: 'ticket_confirmation_accepted' },
  );

  if (accepted) {
    itemsAccepted.add(request.bookingList.length);
  }
}
