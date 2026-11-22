FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Agent OpenTelemetry de Grafana, téléchargé au build (le .jar n'est pas versionné) et vérifié par
# son empreinte. Il n'est actif que si JAVA_TOOL_OPTIONS contient -javaagent (manifestes k8s).
ARG OTEL_AGENT_VERSION=2.31.1
ADD --chmod=644 --checksum=sha256:f17c19ab7f1ffe70a19e97ed502db261ca94ebc5ace5fffcec7f8a890213a4cc \
    https://github.com/grafana/grafana-opentelemetry-java/releases/download/v${OTEL_AGENT_VERSION}/grafana-opentelemetry-java.jar \
    /otel/grafana-opentelemetry-java.jar
# ADD --chmod s'applique aussi au dossier /otel qu'il crée : on rétablit des droits lisibles
# par l'utilisateur non-root (dossier traversable, fichier en lecture)
RUN chmod 755 /otel && chmod 644 /otel/grafana-opentelemetry-java.jar
COPY target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
