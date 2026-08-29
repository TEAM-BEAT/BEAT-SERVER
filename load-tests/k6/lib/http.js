import http from 'k6/http';

export function authorizationHeaders(accessToken) {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

export function assertPreflight(config) {
  const response = http.get(config.preflightUrl, {
    headers: authorizationHeaders(config.accessToken),
    tags: { name: 'load_test_preflight' },
    timeout: '5s',
  });

  if (response.status !== config.preflightExpectedStatus) {
    throw new Error(`Load-test preflight failed with HTTP ${response.status}.`);
  }
}

export { isTimeoutResponse } from './timeout.js';

export function warnIfProduction(config) {
  if (config.targetEnv === 'prod') {
    console.warn(
      '[LOAD TEST WARNING] TARGET_ENV=prod sends traffic to the shared RDS; monitor prod and stop on the documented guard conditions.',
    );
  }
}
