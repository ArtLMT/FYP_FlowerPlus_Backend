FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /var/lib/flowerplus/product-images \
    && chown -R 10001:10001 /var/lib/flowerplus /app
COPY --from=build --chown=10001:10001 /workspace/target/flowerplus-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
