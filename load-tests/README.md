# BEAT 부하 테스트

- k6 실행 패키지: [`k6/README.md`](k6/README.md)
- 일반 API: [`k6/scenarios/http/README.md`](k6/scenarios/http/README.md)
- 예매 확정 DB queue: [`k6/scenarios/ticket-confirmation/README.md`](k6/scenarios/ticket-confirmation/README.md)

조회 API와 단순 CRUD는 범용 HTTP harness를 사용합니다. 고유 mutation 데이터나 queue 적재량처럼
도메인 불변식과 별도 지표가 필요한 흐름만 전용 시나리오로 분리합니다.
