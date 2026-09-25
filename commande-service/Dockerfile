FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY grafana-opentelemetry-java.jar /otel/grafana-opentelemetry-java.jar
EXPOSE 8082
ENTRYPOINT ["java", "-javaagent:/otel/grafana-opentelemetry-java.jar", "-jar", "app.jar"]
