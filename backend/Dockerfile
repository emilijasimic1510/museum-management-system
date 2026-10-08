FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/museum-backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /data/images /data/audio /data/text
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
