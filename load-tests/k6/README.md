# k6

k6는 애플리케이션 밖에서 부하를 생성하는 독립 테스트 도구입니다. 런타임 계측을 담당하는
`observability` Gradle 모듈에 포함하지 않습니다.

## 구조

```text
k6/
└─ scenarios/
   ├─ http/                 # 일반 HTTP API
   └─ ticket-confirmation/  # 예매 확정 전용 불변식과 SMS 부수 효과
```

새 API는 먼저 범용 HTTP 시나리오의 요청 JSON으로 측정합니다. 여러 요청의 순서·동적 토큰·고유
테스트 데이터·외부 부수 효과처럼 별도 규칙이 필요한 경우에만 독립 시나리오를 추가합니다.

모든 시나리오의 k6 지표는 OTLP로 기존 Alloy에 전달됩니다. Spring Boot의 HTTP·JVM·Hikari
지표와 RDS 지표는 기존 수집 경로를 사용하므로 API별 Alloy 설정은 추가하지 않습니다.

비즈니스 처리량처럼 기본 지표에 없는 값만 해당 기능의 애플리케이션 코드에서 Micrometer로
계측합니다. 계측은 부하 테스트 설정에 따라 켜고 끄지 않고 항상 동일하게 동작해야 하며,
Booking ID·사용자 ID 같은 고카디널리티 값은 metric tag로 사용하지 않습니다.
