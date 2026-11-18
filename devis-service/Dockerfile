FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# utilisateur non-root
RUN addgroup -S app && adduser -S app -G app
USER app

COPY target/devis-service-*.jar app.jar

# 8084 : http (hors mTLS) ; 8446 : https mTLS ; 9084 : sondes
EXPOSE 8084 8446 9084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
