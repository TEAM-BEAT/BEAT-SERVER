const TIMEOUT_ERROR_CODES = new Set([1050, 1211]);
const TIMEOUT_ERROR_MESSAGE = /\b(?:timeout|timed out|deadline exceeded)\b/i;

export function isTimeoutResponse(response) {
  const errorCode = Number(response?.error_code);
  if (TIMEOUT_ERROR_CODES.has(errorCode)) {
    return true;
  }

  return typeof response?.error === 'string'
    && TIMEOUT_ERROR_MESSAGE.test(response.error);
}
