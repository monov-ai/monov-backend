# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 캐싱 — gradle 파일만 먼저 복사
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 + bootJar
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Spring Boot layered JAR 분해 — 레이어별 캐싱으로 재배포 빨라짐
RUN java -Djarmode=layertools -jar build/libs/*-SNAPSHOT.jar extract --destination extracted/

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# wget 으로 healthcheck 가능하게 (alpine 은 기본 미포함)
RUN apk add --no-cache wget

# 레이어별 복사 (변경 적은 것 → 변경 잦은 것 순)
COPY --from=build /app/extracted/dependencies/         ./
COPY --from=build /app/extracted/spring-boot-loader/   ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/          ./

# JVM 컨테이너 인지 + 메모리 75% 활용 + G1GC + 한국 시간대
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Duser.timezone=Asia/Seoul"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]