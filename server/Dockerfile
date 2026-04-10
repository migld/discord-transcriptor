FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install sbt
RUN apk add --no-cache bash curl && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.8/sbt-1.9.8.tgz" -o /tmp/sbt.tgz && \
    tar -xzf /tmp/sbt.tgz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt && \
    rm /tmp/sbt.tgz

COPY build.sbt .
COPY project/ project/

# Cache dependencies
RUN sbt update

COPY src/ src/
RUN sbt assembly

# Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/scala-2.13/*-assembly-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
