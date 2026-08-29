export const WORKLOAD_BUDGET_VERSION = 'v1';

const WORKLOAD_BUDGETS = Object.freeze({
  generic_http: Object.freeze({
    hardCap: Object.freeze({ maxRps: 10, maxDurationSeconds: 600 }),
    smoke: Object.freeze({
      executor: 'constant-arrival-rate',
      targetRps: 1,
      peakRps: 1,
      duration: '1m',
      durationSeconds: 60,
      plannedIterations: 60,
      preAllocatedVUs: 10,
      maxVUs: 50,
      gracefulStop: '30s',
    }),
    baseline: Object.freeze({
      executor: 'constant-arrival-rate',
      targetRps: 5,
      peakRps: 5,
      duration: '5m',
      durationSeconds: 300,
      plannedIterations: 1500,
      preAllocatedVUs: 20,
      maxVUs: 100,
      gracefulStop: '30s',
    }),
    step: Object.freeze({
      executor: 'ramping-arrival-rate',
      startRate: 1,
      peakRps: 10,
      duration: '8m',
      durationSeconds: 480,
      plannedIterations: 1620,
      preAllocatedVUs: 20,
      maxVUs: 100,
      gracefulStop: '30s',
      stages: Object.freeze([
        Object.freeze({ target: 1, duration: '2m' }),
        Object.freeze({ target: 2, duration: '2m' }),
        Object.freeze({ target: 5, duration: '2m' }),
        Object.freeze({ target: 10, duration: '2m' }),
      ]),
    }),
  }),
  ticket_confirmation: Object.freeze({
    hardCap: Object.freeze({ maxRps: 1, maxDurationSeconds: 120 }),
    smoke: Object.freeze({
      executor: 'constant-arrival-rate',
      targetRps: 1,
      peakRps: 1,
      duration: '1m',
      durationSeconds: 60,
      plannedIterations: 60,
      preAllocatedVUs: 20,
      maxVUs: 50,
      gracefulStop: '30s',
    }),
    baseline: Object.freeze({
      executor: 'constant-arrival-rate',
      targetRps: 1,
      peakRps: 1,
      duration: '1m',
      durationSeconds: 60,
      plannedIterations: 60,
      preAllocatedVUs: 20,
      maxVUs: 50,
      gracefulStop: '30s',
    }),
    step: Object.freeze({
      executor: 'constant-arrival-rate',
      targetRps: 1,
      peakRps: 1,
      duration: '2m',
      durationSeconds: 120,
      plannedIterations: 120,
      preAllocatedVUs: 20,
      maxVUs: 50,
      gracefulStop: '30s',
    }),
  }),
});

export function getWorkloadBudget(workload, profile) {
  const workloadBudgets = WORKLOAD_BUDGETS[workload];
  if (!workloadBudgets) {
    throw new Error(`Unknown workload: ${workload}.`);
  }

  const budget = workloadBudgets[profile];
  if (!budget) {
    throw new Error(
      `Unknown LOAD_PROFILE=${profile} for ${workload}. Use smoke, baseline, or step.`,
    );
  }

  const maximumRps = budget.stages
    ? Math.max(...budget.stages.map((stage) => stage.target), budget.startRate || 0)
    : budget.targetRps;
  if (maximumRps > workloadBudgets.hardCap.maxRps) {
    throw new Error(`The ${workload} ${profile} profile exceeds its RPS hard cap.`);
  }
  if (budget.durationSeconds > workloadBudgets.hardCap.maxDurationSeconds) {
    throw new Error(`The ${workload} ${profile} profile exceeds its duration hard cap.`);
  }

  return {
    ...budget,
    stages: budget.stages ? budget.stages.map((stage) => ({ ...stage })) : undefined,
    hardCap: { ...workloadBudgets.hardCap },
  };
}
