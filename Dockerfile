# First stage, build the application
FROM eclipse-temurin:25.0.2_10-jdk-alpine AS build
COPY --chown=gradle:gradle ./gradlew /home/gradle/src/
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle gradle/ /home/gradle/src/gradle/
RUN ./gradlew
COPY --chown=gradle:gradle . /home/gradle/src
RUN ./gradlew shadowJar --no-daemon --no-configuration-cache


# Second stage, build the custom JRE
# JDK 25 (JEP 493) no longer ships jmods but jlink can still create custom
# runtime images from the linkable runtime. We use jdeps to discover which
# modules the fat JAR actually needs, then jlink to produce a minimal JRE.
FROM eclipse-temurin:25.0.2_10-jdk-alpine AS jre-builder
COPY --from=build /home/gradle/src/build/libs/*-all.jar /tmp/app.jar
RUN MODULES=$("$JAVA_HOME"/bin/jdeps \
         --print-module-deps \
         --ignore-missing-deps \
         --multi-release 25 \
         /tmp/app.jar) && \
    echo "Discovered modules: $MODULES" && \
    "$JAVA_HOME"/bin/jlink \
         --add-modules "$MODULES,jdk.crypto.ec" \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress zip-6 \
         --output /optimized-jre


# Third stage, use the custom JRE and build the app image
FROM alpine:3.22.0
ENV JAVA_HOME=/opt/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Copy JRE from the builder
COPY --from=jre-builder /optimized-jre "$JAVA_HOME"

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
