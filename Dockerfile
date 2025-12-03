# --- Build stage --- //이미지 생성
FROM gradle:8.8-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# --- Run stage --- //이미지를 기반으로 컨테이너 실행
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","app.jar"]
