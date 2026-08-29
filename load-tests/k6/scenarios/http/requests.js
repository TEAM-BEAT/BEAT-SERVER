import { sha256 } from 'k6/crypto';
import { SharedArray } from 'k6/data';

function validateRequest(request, index) {
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
}

export function loadRequests(env, config) {
  const requestFile = env.REQUEST_FILE || './requests.json';
  const requestSource = open(requestFile);
  const requests = new SharedArray('generic-http-requests', () => JSON.parse(requestSource));
  if (requests.length === 0) {
    throw new Error('REQUEST_FILE must contain at least one request.');
  }
  requests.forEach(validateRequest);

  const reuseRequests = env.REUSE_REQUESTS === 'true';
  if (
    reuseRequests
    && requests.some((request) => !['GET', 'HEAD', 'OPTIONS'].includes(request.method.toUpperCase()))
  ) {
    throw new Error('REUSE_REQUESTS=true is allowed only for read-only HTTP methods.');
  }
  const requiredRequests = config.plannedIterations;
  if (!reuseRequests && requests.length < requiredRequests) {
    throw new Error(
      `At least ${requiredRequests} requests are required for non-reuse mode, but only ${requests.length} were provided.`,
    );
  }
  return { requests, reuseRequests, datasetHash: sha256(requestSource, 'hex') };
}
