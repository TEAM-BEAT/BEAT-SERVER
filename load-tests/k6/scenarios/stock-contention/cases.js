import { sha256 } from 'k6/crypto';
import { SharedArray } from 'k6/data';
import {
  STOCK_CONTENTION_PHASES,
  validateStockCases,
} from './contract.js';

export function loadCases(env, config) {
  const dataFile = env.DATA_FILE || './cases.json';
  const dataSource = open(dataFile);
  const dataset = JSON.parse(dataSource);
  const validated = validateStockCases(dataset);
  const cases = new SharedArray('stock-contention-cases', () => dataset.cases);

  if (!STOCK_CONTENTION_PHASES.includes(config.profile)) {
    throw new Error('stock_contention LOAD_PROFILE must be warmup or flash.');
  }

  const phaseCases = [];
  for (let index = 0; index < cases.length; index += 1) {
    if (cases[index].phase === config.profile) {
      phaseCases.push(cases[index]);
    }
  }
  if (phaseCases.length !== validated[config.profile].length) {
    throw new Error('Loaded stock contention cases do not match the validated dataset.');
  }

  return {
    cases: phaseCases,
    datasetHash: sha256(dataSource, 'hex'),
    counts: {
      warmup: validated.warmup.length,
      flash: validated.flash.length,
    },
    totalCases: validated.totalCases,
  };
}
