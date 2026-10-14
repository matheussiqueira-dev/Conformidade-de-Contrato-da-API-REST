# API do SBI (Spring Boot, Java 25). Usado pelo docker-compose.demo.yml.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Checkout no Windows traz o mvnw com CRLF; o shell do container precisa de LF.
RUN tr -d '\015' < mvnw > mvnw.lf && mv mvnw.lf mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 sbi
WORKDIR /app
COPY --from=build /src/target/order-management-spring-0.0.1-SNAPSHOT.jar app.jar
USER sbi
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
