# First stage, build the custom JRE
FROM eclipse-temurin:21-jdk-alpine AS jre-builder

RUN mkdir /opt/app

WORKDIR /opt/app

# Build small JRE image
RUN "$JAVA_HOME"/bin/jlink \
         --verbose \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /optimized-jdk-21

# Second stage, build the application
FROM eclipse-temurin:21-jdk-alpine AS build
COPY --chown=gradle:gradle ./gradlew /home/gradle/src/
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle gradle/ /home/gradle/src/gradle/
RUN ./gradlew
COPY --chown=gradle:gradle . /home/gradle/src
RUN ./gradlew shadowJar --no-daemon --no-configuration-cache


# Third stage, Use the custom JRE and build the app image
FROM alpine:3.22.0
ENV JAVA_HOME=/opt/jdk/jdk-21
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# copy JRE from the base image
COPY --from=jre-builder /optimized-jdk-21 "$JAVA_HOME"
#FROM eclipse-temurin:21-jre-alpine

# Add app user
ARG APPLICATION_USER=ynab

# Create a user to run the application, don't run as root
RUN addgroup --system "$APPLICATION_USER" &&  \
    adduser --system "$APPLICATION_USER" --ingroup "$APPLICATION_USER" && \
    mkdir /app && \
    chown -R "$APPLICATION_USER" /app

COPY --from=build --chown="$APPLICATION_USER":"$APPLICATION_USER" /home/gradle/src/build/libs/*-all.jar /app/app.jar

WORKDIR /app

USER "$APPLICATION_USER"

ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]
