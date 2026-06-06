FROM gradle:jdk21-jammy AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

LABEL authors="salmadarwiche,ibrahimdanisman"

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /home/gradle/src/build/quarkus-app/ /app/
EXPOSE 8080
ENTRYPOINT ["java","-Dquarkus.profile=prod","-jar","/app/quarkus-run.jar"]
