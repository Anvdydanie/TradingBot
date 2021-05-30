FROM gradle:7.0.2-jdk11 AS build
COPY --chown=gradle:gradle . /src
WORKDIR /src
RUN gradle build --no-daemon

FROM amazoncorretto:11-alpine-jdk

EXPOSE 9999

RUN mkdir /app

COPY --from=build /src/build/libs/*.jar /app/trading-bot-application.jar

CMD ["java", "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+UseContainerSupport", "-XshowSettings:vm", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-jar", "/app/trading-bot-application.jar"]