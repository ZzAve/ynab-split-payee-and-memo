# First stage, build the application
FROM eclipse-temurin:25.0.2_10-jdk-alpine AS build
COPY --chown=gradle:gradle ./gradlew /home/gradle/src/
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle gradle/ /home/gradle/src/gradle/
RUN ./gradlew
COPY --chown=gradle:gradle . /home/gradle/src
RUN ./gradlew shadowJar --no-daemon --no-configuration-cache


# Second stage, build the app image
# Note: JDK 25 (JEP 493) no longer ships jmods, so jlink custom JRE is not
# supported with Temurin alpine images. Using the JRE base image instead.
FROM eclipse-temurin:25.0.2_10-jre-alpine

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
