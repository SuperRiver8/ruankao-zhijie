FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S zhijie && adduser -S zhijie -G zhijie && mkdir -p /data/attachments && chown -R zhijie:zhijie /data
WORKDIR /app
COPY --from=build /app/target/zhijie-api-0.1.0.jar app.jar
USER zhijie
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
