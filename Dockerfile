# API do SBI (Spring Boot, Java 25). Usado pelo docker-compose.demo.yml e pelo Render.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Checkout no Windows traz o mvnw com CRLF; o shell do container precisa de LF.
RUN tr -d '\015' < mvnw > mvnw.lf && mv mvnw.lf mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Jar extraido (app.jar + lib/) carrega classes mais rapido que o jar aninhado e e exigido pelo cache AOT.
RUN ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/order-management-spring-0.0.1-SNAPSHOT.jar extract --destination /extracted \
    && mv /extracted/order-management-spring-0.0.1-SNAPSHOT.jar /extracted/app.jar

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 sbi
WORKDIR /app
COPY --from=build /extracted/ ./
# Partida a frio no Render free (0,1 CPU, 512 MB) passava de 150 s, quase toda em carga de classes e JIT.
# C1 apenas e SerialGC gastam menos CPU na subida; 70% da memoria evita GC seguido com o heap padrao de 25%.
ENV SBI_JVM_FLAGS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:MaxRAMPercentage=70"
# Treino do cache AOT (JEP 483/514/515): sobe o contexto Spring e sai no refresh, sem tocar em banco.
# A senha e ficticia e existe so neste RUN (DatasourcePasswordGuard exige o valor); Flyway e validacao ficam
# desligados aqui e voltam ao normal na execucao. Se JVM ou classpath mudarem, a JVM ignora o cache e sobe sem ele.
RUN SPRING_DATASOURCE_PASSWORD=aot-training java $SBI_JVM_FLAGS -XX:AOTCacheOutput=app.aot \
    -Dspring.context.exit=onRefresh \
    -Dspring.flyway.enabled=false \
    -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -jar app.jar
USER sbi
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $SBI_JVM_FLAGS -XX:AOTCache=app.aot $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
