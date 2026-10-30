# Build context is the backend/ folder:  docker build -f backend/auth-service/Dockerfile backend
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -ntp -q -pl auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
ENV TZ=Asia/Kolkata \
    JAVA_OPTS="-Xms64m -Xmx300m -XX:+UseSerialGC"
WORKDIR /app
RUN apk add --no-cache tzdata && addgroup -S app && adduser -S app -G app
COPY --from=build /src/auth-service/target/auth-service-1.0.0.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
