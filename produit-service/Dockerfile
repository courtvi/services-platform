FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# utilisateur non-root
RUN addgroup -S app && adduser -S app -G app
USER app

COPY target/product-service-*.jar app.jar

EXPOSE 8071 8443 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]