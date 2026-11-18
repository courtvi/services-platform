FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# utilisateur non-root
RUN addgroup -S app && adduser -S app -G app
USER app

# le pom produit un jar "exec" (classifier) : c'est lui qui est exécutable
COPY target/client-service-*-exec.jar app.jar

# 8083 : http (hors mTLS) ; 8445 : https mTLS ; 9083 : sondes
EXPOSE 8083 8445 9083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
