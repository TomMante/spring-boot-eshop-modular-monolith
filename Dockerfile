FROM eclipse-temurin:25-jre-alpine-3.23 AS builder

RUN apk --no-cache add gcompat
ENV LD_PRELOAD=/lib/libgcompat.so.0

WORKDIR /builder
ARG JAR_FILE=./starter/target/starter.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre-alpine-3.23
RUN addgroup -S -g 1002 apiuser && adduser -S -u 1002 -G apiuser apiuser

WORKDIR /application
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/dependencies/ ./
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --chown=apiuser:apiuser --from=builder /builder/extracted/application/ ./

USER apiuser

ENTRYPOINT ["java", "-jar", "application.jar"]
