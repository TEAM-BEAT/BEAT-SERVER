# HANDOFF — session continuity (auto, bounded; human-reviewed per P5)

_Updated: 2026-08-23 06:40_

- Branch: `refactor/cqrs-migration-pr10`
- Last commit: 5dbca59c docs: record final clean migration evidence

## Uncommitted changes
```
?? CLAUDE.md
?? HANDOFF.md
?? batch/src/test/java/
?? docs/architecture/booking-close-processing-adr.md
?? docs/architecture/booking-platform-improvement-report.md
?? docs/architecture/error-handling.md
?? docs/architecture/multi-module-architecture-adr.md
?? docs/architecture/ticket-payment-confirmation-outbox-sqs.md
?? docs/observability/BATCH_LOKI_LOG_DIAGNOSIS.md
?? docs/observability/TRACE_LOG_CORRELATION.md
?? dump.rdb
?? infra/db/
?? java_pid48725.hprof
?? wheel_dir/
```

## Decisions (2026-08-23, owner: move-hoon)

- A1 계좌 스냅샷: **현행 유지 확정** — 계좌 정정은 미입금 예매에 자동 반영이 자연스러움. 금액과 달리 스냅샷 불필요
- A2 legacy 금액: **해결됨(대상 스키마 기준)** — beatDev `booking.total_payment_amount INT NOT NULL` 확인. prod는 추후 DB 마이그레이션 대상(infra/db/migration/*.sql)
- A3 rehydration 감사: **통과** — beatDev 구매수량 1..10 이탈 0건(min 1/max 2), 총 12건. strict validation 게이트 해제 조건 충족
- C1 서버 수량 검증: 이미 구현됨 — `Booking.kt:246` validatePurchaseTicketCount, MAX=10 (`Booking.kt:261`)
- C3 취소·환불 전이: 이미 구현됨 — `Booking.kt:58-130` 상태머신(REFUND_REQUESTED 일반취소 차단 등). improvement-report의 P0 A/B는 코드보다 뒤에 작성된 듯 — 문서 갱신 필요
- C2 예약 생성 멱등성: **열림** — 유일하게 미구현 남은 P0
- SMS 내구성(#568/outbox 설계): 마이그레이션 머지 후 진행
- #581 scan-images 실패: 보류(중요도 낮음 판단)
- #590 머지 전략: 다른 문제 처리 후 결정
- Issue #575/#576/#350은 pr10 브랜치에서 완료(@Jvm* 82+12+6+6개→@JvmInline 9개만 잔존 확인) — develop엔 미반영이라 안 끝나 보인 것. 머지 후 close

## Progress log (single-batch execution)

- ✅ 테스트명 한국어화 완료: 한글 미포함 테스트명 **0개**(서브에이전트 57파일/~195건 + 가드 6파일 직접). 스타일=코드믹스("command 패키지는 query 패키지에 의존하지 않는다"). AGENTS.md 규칙 갱신. 컴파일+:domain:test green
- ✅ B4/B6/B8: timeout 상수화, Booking.create/rehydrate non-null화(null가드 테스트→컴파일 보장), BookingCreationResult nullable 10→4
- ✅ 시간 고정: BeatTestContainersConfig @Primary testClock(FIXED_NOW=2026-08-23T09:00 KST) + 3동시성테스트 NOW 상수화(예매마감=DB시간이라 일정 fixture만 실시간). AGENTS.md 규칙 추가
- ✅ R: 루트 계약스펙 삭제+transitionBoundaryTest 제거+루트 junit 제거
- ✅ A2+A3: ArchUnit 전면 재작성(40+→26룰, 중복/공문서 룰 삭제, KDoc로 계약 명시)+FunSpec+한국어 코드믹스명
- ✅ A4+B2+B3+B1: 죽은 필드3/legacyUrls/performImages/LEGACY_SECRET 제거
- ⏭️ 남음: B7(facade requireNotNull 72→DTO non-null) · C6 CDN serializer DI · C7 TTL/쿠키 상수 · C8 origin filter 경로 · C10 cleanup 청킹 · C14 domain junit 의존(+루트 재확인) · F9 MockK 도입·전환 · P 물리정렬(P1디렉터리/P2워크플로우/패키지 rename) · D문서(D1~D6+ADR-FINAL-001 개정) · OpenAPI baseline 재생성+breaking diff · 최종게이트(full check+bootjars+actionlint)

## Execution strategy (owner decision, 2026-08-23)

## Execution strategy (owner decision, 2026-08-23)

- **R 항목 추가**: 루트 src/test 계약스펙 3종(Deployment/Runtime/BuildTooling)은 소유자 판정상 레거시 덩어리 → 이번 배치에서 삭제. transitionBoundaryTest 태스크+CI 참조+루트 junit 의존 동반 제거. P3(계약스펙 경로갱신) 항목은 소멸
- 발견 버그: application/admin에 JUnit 엔진 부재로 AdminApplicationArchitectureTest 미실행(XML 증거). FunSpec 전환으로 해결 예정

- **단일 배치 실행**: 머지 후 청소 PR 분할 없음. A~P 전부 현재 브랜치에서 완료 후 마이그레이션과 함께 한 번에 머지
- **호환/폴백 금지**: legacy fallback·alias·듀얼라이트·deprecation 윈도우 만들지 않음 — 바로 제거/반영 (클라 호환 100% 검증済이므로 안전)
- 물리 정렬(P)도 동일 배치 포함. 패키지 이동은 소유자 명시 계획(디렉터리+Kotlin 패키지 전부)

## Corrections 2 (2026-08-23)

- **F9 MockK 전환 가능으로 변경**: JDK25+MockK 1.14.9+(byte-buddy 1.18.x)=Java 25 지원. 실사용 사례(UK MoJ hmpps-integration-api: JVM_25 target + MockK 1.14.11 + JDK25 CI). 이슈 #1434는 구버전 byte-buddy 1.15.11 기준이라 stale. 절차: 카탈로그 등록(현재 mockk 미등록 확인) → `dependencyInsight --dependency byte-buddy`로 1.18.x 확인 → Mockito double 단계 전환 → FINAL-REPORT §16 해당 리스크 삭제
- **물리 이동(디렉터리/패키지 정렬) = 소유자 계획 확정**: ADR-FINAL-001의 "rename 안 함"은 최종 결정이 아니었음. 재검토 시 걸리는 지점 4곳 확인: ①settings.gradle.kts:39-45 projectDir 7매핑 ②워크플로우 경로필터(deploy-dev.yml:91-96 외 rollback/prod/ci-pr/ansible-lint) ③루트 계약스펙 3종이 apis/build.gradle.kts·core/infra/** 직접 read ④Kotlin 패키지명(com.beat.apis 등) rename 범위. → #590 머지 후 별도 PR로 ADR-FINAL-001 개정과 함께 실행

## Corrections (2026-08-23, owner feedback)

- **B5 기각(오탐)**: `cloud.cdn.domain`은 추적 파일 `core/infra/src/main/resources/application-external.yml`에 프로필별로 존재 — dev=https://cdn.dev.beatlive.kr / prod=https://cdn.beatlive.kr / test=https://test-cdn.beatlive.kr. develop↔HEAD diff 없음. 서브에이전트 검색 범위 결함(apis 리소스만 스캔). 응답 이미지 URL은 실제 CDN 도메인으로 리라이트됨 — 양 브랜치 동일 동작이라 클라 호환 결론은 불변(메커니즘 설명만 정정)
- **미추적 파일 = 소유자 의도**(민감정보·코드베이스 청결 정책): infra/db/migration SQL, 문서 7종 등 보고서에서 "커밋 강제" 항목 전부 제외. 백업 필요성만 소유자 재량
- 교훈: 모듈 간 설정은 core/infra 공유 yml에 있음 — 설정 키 검색 시 전 모듈 resources 대상

## Client compatibility verdict (2026-08-23)

- BEAT-Client(develop, React/TS) ↔ pr10 HEAD: **와이어 100% 호환 확정** — 라우트/DTO 필드/enum 리터럴(genre·bankName 16종·scheduleNumber FIRST~TENTH·bookingStatus 5종)/캐러셀 discriminator("generate"/"modify")/에러 봉투(status=message=HTTP 동기)/404-as-result/409 availability 전부 일치
- 죽은 필드(scheduleNumber·totalPaymentAmount·bookingStatus)의 전송자 = Book.tsx:165-169. Jackson 미확인필드 무시 → **서버 단독 제거 안전** (OpenAPI baseline 갱신 필요)
- `legacyUrls` 클라 미참조 0회 → 응답 제거 안전. `performImages` alias도 canonical 키 동시 전송으로 제거 가능
- 클라 선존재 버그(migration 무관): AuthRequierd.tsx `Authorization_Refresh` 헤더 플로우는 양쪽 서버 모두 미지원 죽은 경로(실동작=useTokenRefresher 쿠키 방식). Book.tsx:275 위치 인덱싱은 scheduleId 연속성 가정
- refresh-token 엔드포인트: @CookieValue 방식, develop↔HEAD 바이트 동일

## Next
- C2 멱등성 구현 착수 여부/시점 확정
- 마이그레이션 머지 후: stale issue 3종 close(#575/#576/#350), FINAL-REPORT §16 갱신(A1~A3 해결 반영)
- 첫 청소 PR 후보: 죽은 필드 제거 + legacyUrls/performImages alias 제거 + OpenAPI baseline 갱신

## Full audit (2026-08-23, 3 subagents + cross-check)

HIGH: GuestBookingRequest/MemberBookingRequest의 scheduleNumber·totalPaymentAmount·bookingStatus 필드 어디서도 안 읽힘(계약 오염). PROD_JWT_LEGACY_SECRET 소비처 0(고아).
진짜 Constitution 위반 2: §37 readmodel 가드 발화 불가(패키지 없음, FrontofficeApplicationArchitectureTest.kt:232), 신규 ArchUnit 6종 JUnit 작성(§34.1 FunSpec 위반).
그레이: Mockito 29spec/MockK 0(문서는 MockK 원칙), §8/§9 settlement·payment·statistics 문서에만 존재, application public≫internal.
정리 대상: facade requireNotNull ~73(@Valid 이중), SuccessResponse 등 바이트동일 복제×3, 예외핸들러 쌍 ~180줄, Result/Results·Reader/Queries 명명 드리프트, timeout=200 매직넘버(MemberBookingCommandService.kt:31), cloud.cdn.domain 키 미설정, legacyUrls/performImages alias 무문서, hprof/dump.rdb/app.jar junk.
클린 확인: TODO/FIXME 0, Thread.sleep(prod) 0, empty catch 0, lock order 전 준수, TX 내 외부호출 0, dead endpoint 0, domain 순수, 10-project graph/포트소유권/CQRS/에러경계 전 IMPLEMENTED.

## Actuator path — 해결 완료 (2026-08-23, 커밋 9e33f59d)
- 경로 모델(소유자 확인): test=/actuator-test(yml 고정), dev/prod=시크릿 \${DEV|PROD_ACTUATOR_PATH} ← ansible env.yml이 주입. SecurityConfig들은 프로퍼티 주입이라 원래 정상.
- 수정: AccessLogEmitter·BaseMdcLoggingFilter·SecurityMdcLoggingFilter 체인에 basePath 전달, SecurityFilterConfig가 \${management.endpoints.web.base-path:/actuator} 주입 → 전 환경 자동 대응. AccessLogEmitterTest에 기본/설정 이중 케이스 추가. SecurityMdcLoggingFilter는 포트 기반 판정이라 경로 무관(테스트 URI는 장식).
- 동반 커밋 04afa2f2: 관측모듈 테스트명 한국어화 61건(스캔 범위 누락분 — 소유자 지적로 발견). ApplicationTest Import 검증 허용목록 전환 포함.

## MDC↔JWT 순서 — Finding 철회 (2026-08-23)
- 현행(MDC first, finally에서 refreshUserIdInMdc)은 의도된 모범 사례. 근거: BaseMdcLoggingFilter.finally 주석+코드, SheldonIp 블로그(로깅필터는 security chain 앞에), Spring Security Architecture(사용자 필요시 인증필터 뒤=late binding), GH#31142(이중등록 방지 패턴 일치).
- 스왑 금지: JWT 선배치 시 Rejected(401) 요청이 MDC/access-log 체인 미도달로 관측성 상실.
- 비회원=GUEST 기본값/회원=finally late-binding 설계 확인. 재플래깅 금지.

## Filter chain verification (2026-08-23)
- 필터 전수 해설·플로우별 검증 완료(회원/비회원/거절/프로브). 현행 순서=MDC-first(관측)→guestOrigin(api)→JWT(인증)→Authorization. finally late-binding으로 userId 해결. Finding 철회 확정(8d4bdf3c).
- B7 커밋 3b788c7b: 요청 DTO non-null화(19개)+facade requireNotNull 72→0+응답 userId tightening+JSON스펙 매퍼 프로덕션 정렬. apis 테스트 green.
- ⏭️ 남음: C6(CDN serializer DI)/C7(TTL·쿠키 상수)/C8(origin filter 경로)/C10(cleanup 청킹)/C14(domain junit 제거)/F9 MockK/P 물리정렬/D문서/OpenAPI 재생성/최종게이트

## JSON 스택 판정 (2026-08-23)
- 프로덕션 바인딩 실증: MockMvc PUT /tickets/update 필수필드 누락 → **400**(TicketControllerWebSpec 일회성 프루브, 미커밋). Boot4+Jackson3+Kotlin 모듈이 non-null 파라미터를 경계에서 강제함 → B7 전제 확정.
- kotlinx.serialization 평가: **Jackson 3 유지 확정**. Boot4 일급(tools.jackson)이라 자동설정·springdoc·컨버터 생태계 정합 / 교체 시 @JsonProperty·@JsonTypeInfo(캐러셀 판별자)·ValueSerializer(CDN) 전면 재작성 비용 / 얻는 것은 단일 서버 타겟에서 미미.
- 잔여 추적: jackson-module-kotlin:2.21.5(Jackson2 세대)가 여전히 runtimeClasspath에 존재 — 누가 끌어오는지 추출해 퇴출 과제로.

## Q2/Q3 근거 + 앵커 체이닝 (2026-08-23)
- 결론: Kotlin 타입=필수성, Bean Validation=범위/형식(@Size 등) 역할 분담이 권장(하이브리드). @NotNull 전면 퇴출 완료, jakarta-validation 의존성은 @Size/@Valid cascade 위해 유지. 근거: spring.io Kotlin Annotations 문서, jackson-module-kotlin 동작(Baeldung), SO 41993706/54797207.
- ApisSecurityConfig JWT 앵커를 guestSessionOriginFilter로 명시 체이닝(a2cab58b) — fail-fast 개선, matrix 스펙 green.
- Loki elapsed_ms는 문자열 저장 — 숫자 비교 쿼리 시 duration() 변환 사용(운영 팁, 코드 변경 불요).

## scheduler.owner 제거 + yml 컨벤션 (2026-08-23, 0db47f77+87151530)
- beat.scheduler.owner 전면 제거: 잡 2개 가드/주입, batch test yml(삭제), BootSpec ownership 단언, job 스펙 2개→단일 위임 계약, ansible env 조건+bluegreen/stopstart 변수+dev/prod inventory 4줄. 근거: @Scheduled는 ArchUnit이 batch 전용 보장, 플래그는 레플리카 HA 미해결 가짜 안전감. HA 필요 시 ShedLock/리더선출 별도 설계.
- yml 배치 실태: 앱별 src/test/resources/application-test.yml 이미 표준 구조(batch는 flag만 있어 삭제). main yml엔 test 인라인 없음 ✅. 공유 기본값은 observability 라이브러리 프로필 블록(정당한 예외).
- AGENTS.md는 gitignore라 컨벤션 문구는 본 문서로 대체: "앱 고유 테스트 오버라이드=src/test/resources/application-test.yml / 공유 기본값=observability 프로필 블록 / main yml에 on-profile:test 인라인 금지".
