# BEAT 부하 테스트

- 일반 API: [`http/README.md`](http/README.md) — 요청 JSON만 바꿔 공통 HTTP·JVM·Hikari·RDS 지표 확인
- 예매 확정: [`booking-confirmation/README.md`](booking-confirmation/README.md) — 고유 Booking과 실제 SMS 차단까지 검증

조회 API와 단순 CRUD는 범용 HTTP harness를 사용합니다. 외부 결제·SMS처럼 부수 효과가 있거나
테스트 데이터 불변식이 필요한 흐름은 전용 시나리오를 추가합니다.
