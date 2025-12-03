FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Install compatibility library for glibc binaries (protoc)
RUN apk add --no-cache gcompat

COPY product-query-service/gradle gradle
COPY product-query-service/gradlew .
COPY product-query-service/build.gradle.kts .
COPY product-query-service/settings.gradle.kts .

RUN chmod +x ./gradlew
# Download dependencies - this layer will be cached unless build.gradle.kts changes
RUN ./gradlew dependencies --no-daemon || true

COPY product-query-service/src src
COPY proto proto

RUN ./gradlew bootJar -x test
RUN mkdir -p build/libs && ls -la build/libs

FROM eclipse-temurin:21-jre-alpine
# Install compatibility library in runtime image too if needed (usually not for java, but good for debug)
RUN apk add --no-cache gcompat
VOLUME /tmp
COPY --from=build /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
