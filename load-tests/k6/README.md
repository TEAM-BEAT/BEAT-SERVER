# k6

k6는 애플리케이션 밖에서 부하를 생성하는 독립 테스트 도구입니다. 런타임 계측을 담당하는
`observability` Gradle 모듈에 포함하지 않습니다.

## 구조

```text
k6/
├─ lib/                     # 공통 안전 설정과 HTTP preflight
└─ scenarios/
   ├─ http/                 # 일반 HTTP API
   └─ ticket-confirmation/  # 예매 확정 DB queue 전환 성능 실험
```

새 API는 먼저 범용 HTTP 시나리오의 요청 JSON으로 측정합니다. 여러 요청의 순서, 고유 mutation
데이터, queue job 수처럼 별도 불변식과 지표가 필요한 경우에만 독립 시나리오를 추가합니다.

공유 dev RDS에서 실행하는 기본 설정은 최대 `2 RPS / 5분`으로 제한됩니다. 이 환경의 결과는
두 구현을 같은 조건에서 비교하는 저부하 특성 측정이며, 최대 처리량이나 운영 용량의 근거가
아닙니다. 용량 시험은 운영과 분리된 DB와 동일한 서버 사양을 갖춘 전용 환경에서 수행합니다.

모든 시나리오의 k6 지표는 OTLP로 기존 Alloy에 전달됩니다. Spring Boot의 HTTP·JVM·Hikari
지표와 RDS 지표는 기존 수집 경로를 사용하므로 API별 Alloy 설정은 추가하지 않습니다.

비즈니스 처리량처럼 기본 지표에 없는 값만 해당 기능의 애플리케이션 코드에서 Micrometer로
계측합니다. 계측은 부하 테스트 설정에 따라 켜고 끄지 않고 항상 동일하게 동작해야 하며,
Booking ID·사용자 ID 같은 고카디널리티 값은 metric tag로 사용하지 않습니다.
