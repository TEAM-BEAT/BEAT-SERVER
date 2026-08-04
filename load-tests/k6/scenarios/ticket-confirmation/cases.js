import { SharedArray } from 'k6/data';

export function loadCases(env, config) {
  const itemsPerRequest = Number(env.ITEMS_PER_REQUEST || 1);
  if (!Number.isInteger(itemsPerRequest) || itemsPerRequest < 1) {
    throw new Error('ITEMS_PER_REQUEST must be a positive integer.');
  }

  const cases = new SharedArray('ticket-confirmation-db-queue-cases', () =>
    JSON.parse(open(env.DATA_FILE || './cases.json')),
  );
  const requiredCases = config.targetRps * config.durationSeconds;
  if (cases.length < requiredCases) {
    throw new Error(`At least ${requiredCases} unique cases are required, but only ${cases.length} were provided.`);
  }

  const bookingIds = new Set();
  cases.forEach((request, requestIndex) => {
    if (!Number.isInteger(request.performanceId) || request.performanceId < 1) {
      throw new Error(`Invalid performanceId at request index=${requestIndex}.`);
    }
    if (!Array.isArray(request.bookingList) || request.bookingList.length !== itemsPerRequest) {
      throw new Error(
        `Invalid item count at request index=${requestIndex}: expected=${itemsPerRequest}, actual=${request.bookingList?.length}`,
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
  return cases;
}
