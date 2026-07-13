# deploy-vps 1.1/1.3/8.1: single build, single artifact. Build context is the repo root so
# comanda-api's pom.xml can reach comanda-client as a sibling directory (frontend-maven-plugin
# `${frontend.dir}`) and produce one JAR with the SPA embedded.
FROM maven:3.9-eclipse-temurin-21 AS build
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
