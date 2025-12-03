FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

COPY product-command-service/gradle gradle
COPY product-command-service/gradlew .
COPY product-command-service/build.gradle.kts .
COPY product-command-service/settings.gradle.kts .
COPY product-command-service/src src
COPY proto proto

RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test
RUN mkdir -p build/libs && ls -la build/libs

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
