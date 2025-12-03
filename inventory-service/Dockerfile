FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

COPY inventory-service/gradle gradle
COPY inventory-service/gradlew .
COPY inventory-service/build.gradle.kts .
COPY inventory-service/settings.gradle.kts .
COPY inventory-service/src src
COPY proto proto

RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test
RUN mkdir -p build/libs && ls -la build/libs

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
