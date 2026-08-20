# BEAT-SERVER Architecture Migration Artifact

## Safety and baseline

- [x] Target architecture document read completely
- [x] Existing dirty worktree detected and preserved
- [x] Base branch and baseline commit recorded
- [x] Existing uncommitted migration changes classified
- [x] Clean `develop` baseline build/test result recorded

## Required initial audit

- [x] Full Gradle project/dependency graph audited
- [x] CI/CD and runtime assumptions audited
- [x] Application Services inventoried and ownership-classified
- [x] `module-contracts` interfaces/read models inventoried and classified
- [x] Major Command correctness paths traced to consistency sources
- [x] Architecture tests and root legacy tests inventoried
- [x] Java/Kotlin interoperability debt inventoried
- [x] Java production and test sources inventoried
- [x] Reference Capability selected with evidence

## PR strategy

- [x] PR dependency graph recorded
- [x] Every PR has boundary rationale, risks, tests, dependencies, rollback, and DoD
- [x] Quarantined experimental changes re-diffed and mapped to the revised PR boundaries

## Migration implementation

- [ ] Correctness/characterization prerequisites complete
  - [x] Member Booking response identity corrected and verified on the migration branch
  - [ ] Snapshot, rehydration, Guest identity, and concurrency decisions resolved
- [x] Target Gradle skeleton and initial graph guards complete
- [ ] Final executable dependency and source-boundary guards complete
- [ ] Booking reference Capability collaboration corrected and fully verified
- [ ] Remaining frontoffice capabilities migrated and verified
- [ ] Admin workflows migrated and verified
- [ ] System/batch workflows migrated and verified
- [ ] Infrastructure consolidated with internal implementations
- [ ] Security and observability boundaries normalized
- [ ] `module-contracts` consumers migrated and module retired
- [ ] Kotlin-owned APIs modernized after caller audit
- [ ] Legacy architecture tests replaced and retired

## Final verification

- [x] `./gradlew check` passes
- [x] Applicable unit/application/infrastructure/security/batch/API tests pass
- [x] All three apps build and remain independently executable
- [ ] Final Gradle dependency graph inspected
- [ ] Application public APIs inspected and minimized
- [ ] No `module-contracts` or legacy module references remain
- [ ] Remaining Java sources and `@Jvm*`/`Optional` usages justified
- [ ] Source-string architecture assertions removed or justified
- [ ] Command-to-read-model correctness dependencies absent or justified
- [ ] Cross-Application-Service calls absent or justified
- [ ] Public infrastructure implementations absent or justified
- [ ] Temporary migration adapters absent or justified
- [ ] Final migration report complete

## Evidence log

- Baseline source: clean `develop` Git object `eb007147f6aa3824073b108407ea3ae47748aa40`. It contains only the legacy projects; target-project aliases/application lanes in the current worktree are experimental changes and are not baseline evidence.
- Clean baseline verification: `./gradlew check verifyModuleBootJars --no-daemon --max-workers=1` passed in 3m 28s with 98 executed tasks. Three Infra Kotlin compiler warnings remain recorded as debt.
- Application ownership: Booker Booking/Schedule/Home, Booker+Maker Performance, Maker Ticket, Member/Auth, Admin Promotion/User, and System Booking/Promotion maintenance were identified. File upload preparation belongs semantically to Performance Maker, not an independent Domain capability.
- Constitution re-audit, source inventory, revised 14-PR graph, contract disposition, and reopened collaboration decision: `docs/architecture/BEAT-SERVER-MIGRATION-EXECUTION.md`.
- Guest password hashing correction: it is a `support:security` public technical API with an internal BCrypt implementation, not an Application output port. Revised PR-4 owns this transition and forbids `support:security → application`.
- Rejected `application.frontoffice.performance.api`/offer experiment and its package guard were removed. Booking now uses the approved authoritative Domain repository collaboration; no `performance/api` remains.
- Concurrency correction: preliminary `Schedule.findById` caused stale JPA first-level-cache inventory and reproduced 30/30 successes where only 5 were valid. It was replaced by `ScheduleRepository.findPerformanceIdById(Long): Long?`; locked Schedule membership/inventory remains the final authority. The focused concurrency test then passed.
- Main verification: forced targeted Application/support/API concurrency tests and `verifyTargetModuleGraph` passed with 66 executed tasks. Full `check verifyModuleBootJars` is rerun after this correction before any completion box is checked.
- Full current-boundary verification: `./gradlew check verifyModuleBootJars --no-daemon --max-workers=1` passed in 2m 49s; 118 tasks, all three executable boot jars verified. These gates must be rerun after later PRs.
- Correctness: the current Performance summary adapter reads primary DB state, but its mixed `@ReadModel` contract is unsafe for money/authorization commands. Member Booking response identity, legacy amount fallback, rehydration range, guest identity, lock ordering, and Promotion referential concurrency remain implementation prerequisites.
- The rejected Performance API experiment is no longer quarantined because it was deleted. Remaining migration changes are accepted only at their individual verified PR boundary; unrelated pre-existing untracked files remain untouched.

## Quarantined experimental work

Earlier PR-1/PR-2/PR-3 work was re-diffed against the approved report. The target skeleton and Booking slice are verified incrementally; this does not convert unresolved correctness questions or later Capability work into completed migration items.
