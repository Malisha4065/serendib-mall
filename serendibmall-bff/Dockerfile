FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

COPY serendibmall-bff/gradle gradle
COPY serendibmall-bff/gradlew .
COPY serendibmall-bff/build.gradle.kts .
COPY serendibmall-bff/settings.gradle.kts .
COPY serendibmall-bff/src src
COPY proto proto

RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test
RUN mkdir -p build/libs && ls -la build/libs

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
