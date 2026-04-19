FROM gradle:8.7-jdk17 AS build
WORKDIR /src
COPY . .
RUN gradle --no-daemon shadowJar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /src/build/libs/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]