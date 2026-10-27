FROM eclipse-temurin:25-jdk-alpine-3.23 AS builder

WORKDIR /builder
COPY . .
# Tests are skipped: the suite needs Docker for Testcontainers, which an image build doesn't have. CI runs them.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl starter -am package -DskipTests
RUN cp starter/target/starter.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre-alpine-3.23
RUN addgroup -S -g 1002 apiuser && adduser -S -u 1002 -G apiuser apiuser

WORKDIR /application
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/dependencies/ ./
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/application/ ./

USER apiuser

ENTRYPOINT ["java", "-jar", "application.jar"]
