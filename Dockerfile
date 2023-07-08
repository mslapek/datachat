FROM gradle:8.2.0-jdk17 AS build

COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM eclipse-temurin:17-jre AS run

EXPOSE 8080

COPY --from=build /home/gradle/src/webserver/build/libs/ /app.jar
CMD ["java", "-jar", "/app.jar/webserver-0.0.1.jar"]
