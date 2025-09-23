# --- Build stage ---
FROM gradle:8.8-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# --- Run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# 1) 애플리케이션 JAR 복사
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# 2) Elastic APM Agent 추가 (버전 맞춰서 다운로드)
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.51.0/elastic-apm-agent-1.51.0.jar /app/elastic-apm-agent.jar

# 3) 환경변수 (여기서 커스터마이징)
ENV ELASTIC_APM_SERVICE_NAME=youthjob-be
ENV ELASTIC_APM_ENVIRONMENT=prod
ENV ELASTIC_APM_SERVER_URLS=http://apm-server:8200
ENV ELASTIC_APM_APPLICATION_PACKAGES=com.youthjob
# 샘플링 비율 조정 가능 (0.1 = 10% 샘플링)
ENV ELASTIC_APM_TRANSACTION_SAMPLE_RATE=1.0

EXPOSE 8080

# 4) javaagent 옵션 추가
ENTRYPOINT ["java",
    "-Duser.timezone=Asia/Seoul",
    "-javaagent:/app/elastic-apm-agent.jar",
    "-jar","app.jar"]
