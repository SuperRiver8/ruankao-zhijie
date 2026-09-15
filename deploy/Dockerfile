FROM eclipse-temurin:21-jre-alpine
ENV TZ=Asia/Shanghai
RUN addgroup -S zhijie && adduser -S zhijie -G zhijie && mkdir -p /data/attachments && chown -R zhijie:zhijie /data
WORKDIR /app
COPY app.jar /app/app.jar
USER zhijie
EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Asia/Shanghai","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
