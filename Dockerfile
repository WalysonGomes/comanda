# Build from the repository root so Maven can compile the frontend sibling project and
# package the generated SPA into the Spring Boot JAR.
FROM maven:3.9-eclipse-temurin-21 AS build
ARG APP_DOMAIN
ARG VITE_TENANT_DOMAIN
ARG VITE_ROOT_HOST_ALIASES=""
RUN test -n "$APP_DOMAIN" || (echo "APP_DOMAIN build argument is required" >&2; exit 1)
ENV APP_DOMAIN=$APP_DOMAIN
ENV VITE_TENANT_DOMAIN=${VITE_TENANT_DOMAIN:-$APP_DOMAIN}
ENV VITE_ROOT_HOST_ALIASES=$VITE_ROOT_HOST_ALIASES
WORKDIR /workspace
COPY comanda-client comanda-client
COPY comanda-api comanda-api
WORKDIR /workspace/comanda-api
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S comanda && adduser -S comanda -G comanda
WORKDIR /app
COPY --from=build /workspace/comanda-api/target/comanda-api-*.jar app.jar
USER comanda
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
