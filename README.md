# BEAT-SERVER

## 💗 BEAT Information 💗

```
모두를 위한, 그래서 대학생을 위한 공연 예매 플랫폼
```

## 🥁 BEAT Server Developers 🥁

|                                                                                                    이동훈                                                                                                    | 황혜린 | 
|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:| :---: | 
|                                    <img width="200" alt="branch" src="https://github.com/TEAM-BEAT/BEAT-SERVER/assets/144998449/43e18ca0-5eca-46bf-a108-db662cb3ce9b">                                    |<img width="200" alt="branch" src="https://github.com/TEAM-BEAT/BEAT-SERVER/assets/144998449/e678429a-6a1b-4a0b-8457-7eb4b6f9de29"> | 
|                                                                                [hoonyworld](https://github.com/hoonyworld)                                                                                | [hyerinhwang-sailin](https://github.com/hyerinhwang-sailin) |
| prod 서버용 EC2, RDS 구축 <br> dev, prod github action <br> CI 구축(~dockerhub push) <br>Jenkins multibranch pipeline<br> dev 서버에서 dev, prod CD <br> prod 서버 nginx 무중단배포<br> ERD 및 DB 설계 <br> Entity 초기 세팅 | dev 서버용 EC2, RDS 구축 <br> ERD 및 DB 설계 <br> 인증 / 인가 구현 (Redis) <br> 웹 발신 <br> Entity 초기 세팅 <br> 카카오 소셜 로그인 <br> Swagger 세팅 | 

### 🏡 Git Convention
[Git Convention](https://www.notion.so/jiwoothejay/git-convention-9bee60c3bb0a45f1913616b3e72b87b7)

### 💬 Code Convention
[Code Convention](https://www.notion.so/jiwoothejay/spring-code-convention-15be5fc539a14196b2c360ebfb373856)

### 🌳 Commit Convention
[Commit Convention](https://www.notion.so/jiwoothejay/issue-pr-templates-44f118ed82904febae246518ef150e25)

### 📁 Foldering
```
       src
	├── main
	│   ├── java
	│   │   └── com
	│   │       └── beat
	│   │           ├── domain
	│   │           │   ├── booking
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           │   ├── cast
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           │   ├── member
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           │   ├── performance
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           │   ├── promotion
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           │   ├── schedule
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           │   ├── staff
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           │   ├── users
	│   │           │   │   ├── api
	│   │           │   │   ├── application
	│   │           │   │   ├── dao
	│   │           │   │   ├── domain
	│   │           │   │   └── exception
	│   │           ├── global
	│   │           │   ├── common
	│   │           │   │   ├── config
	│   │           │   │   ├── dto
	│   │           │   │   └── exception
	│   │           │   │       ├── base
	│   │           │   │       └── handler
	│   │           │   ├── auth
	│   │           │   │   ├── feign
	│   │           │   │   │   └── kakao
	│   │           │   │   ├── filter
	│   │           │   │   ├── jwt
	│   │           │   │   ├── redis
	│   │           │   │   ├── security
	│   │           │   ├── external
	│   │           │   │   ├── discord
	│   │           │   │   │   ├── exception
	│   │           │   │   │   └── model
	│   │           │   │   ├── s3
	│   │           │   │   │   ├── dto
	│   │           │   │   │   ├── exception
	│   │           │   │   │   └── service
	│   │           ├── infra
	│   │           │   ├── email
	│   │           │   └── sms
	│   │           └── BeatApplication
	│   └── resources
	│       ├── application.yml
	│       ├── application-dev.yml
	│       ├── application-local.yml
	│       └── application-prod.yml
```

## 🔗 ERD
<img width="1131" alt="erd_beat" src="https://github.com/TEAM-BEAT/BEAT-SERVER/assets/125895298/a3dd8812-bc55-4680-b497-942f9213a8f6">

## 📄 API 명세서
[API specification](https://jiwoothejay.notion.site/1-API-c34df726fd464899a31841d35a69ab32?pvs=4)

## 🛠️ Tech

## 🔨 Architecture
<img width="918" alt="image" src="https://github.com/user-attachments/assets/14575f6d-4f3d-4b54-9c3c-6a302db2a5a4">
