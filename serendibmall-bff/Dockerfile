FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Install compatibility library for glibc binaries (protoc)
RUN apk add --no-cache gcompat

COPY serendibmall-bff/gradle gradle
COPY serendibmall-bff/gradlew .
COPY serendibmall-bff/build.gradle.kts .
COPY serendibmall-bff/settings.gradle.kts .

RUN chmod +x ./gradlew
# Download dependencies - this layer will be cached unless build.gradle.kts changes
RUN ./gradlew dependencies --no-daemon || true

COPY serendibmall-bff/src src
COPY proto proto

RUN ./gradlew bootJar -x test
RUN mkdir -p build/libs && ls -la build/libs

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache gcompat
VOLUME /tmp
COPY --from=build /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
