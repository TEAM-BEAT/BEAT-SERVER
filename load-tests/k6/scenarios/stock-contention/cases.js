import { sha256 } from 'k6/crypto';
import {
  buildStockContentionRequest,
  STOCK_CONTENTION_CASE_COUNTS,
  STOCK_CONTENTION_PHASES,
  validateStockCases,
} from './contract.js';

export function loadCases(env, config) {
  const dataFile = env.DATA_FILE || './cases.json';
  const dataSource = open(dataFile);
  const dataset = JSON.parse(dataSource);
  const validated = validateStockCases(dataset);

  if (!STOCK_CONTENTION_PHASES.includes(config.profile)) {
    throw new Error('stock_contention LOAD_PROFILE must be warmup or flash.');
  }

  return {
    accessToken: validated.accessToken,
    request: buildStockContentionRequest(validated, config.profile),
    phaseCaseCount: STOCK_CONTENTION_CASE_COUNTS[config.profile],
    datasetHash: sha256(dataSource, 'hex'),
    totalCases: validated.totalCases,
  };
}
