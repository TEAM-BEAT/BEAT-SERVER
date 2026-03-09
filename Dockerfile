FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY ./build/libs/beat-0.0.1-SNAPSHOT.jar /app/beat.jar

CMD ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=prod", "-jar", "/app/beat.jar"]
