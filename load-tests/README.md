# BEAT 부하 테스트

- k6 실행 패키지: [`k6/README.md`](k6/README.md)
- 일반 API: [`k6/scenarios/http/README.md`](k6/scenarios/http/README.md)
- 예매 확정: [`k6/scenarios/ticket-confirmation/README.md`](k6/scenarios/ticket-confirmation/README.md)

조회 API와 단순 CRUD는 범용 HTTP harness를 사용합니다. 외부 결제·SMS처럼 부수 효과가 있거나
테스트 데이터 불변식이 필요한 흐름은 전용 시나리오를 추가합니다.
