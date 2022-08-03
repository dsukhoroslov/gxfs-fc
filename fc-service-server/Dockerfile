#
# Build stage
#
FROM maven:3.8.4-jdk-11-slim AS build

COPY ../pom.xml /home/app/fc-service

COPY ../fc-service-api/pom.xml /home/app/fc-service/fc-service-api
RUN mvn -f /home/app/fc-service/fc-service-api/pom.xml clean package

COPY ../fc-service-core/pom.xml /home/app/fc-service/fc-service-core
RUN mvn -f /home/app/fc-service/fc-service-core/pom.xml clean package

COPY pom.xml /home/app/fc-service/fc-service-server
RUN mvn -f /home/app/fc-service/fc-service-server/pom.xml clean package

#
#Package stage
#
FROM openjdk:11-jre-slim
COPY --from=build /home/app/fc-service/fc-service-server/target/fc-service-server.jar /usr/local/lib/fc-service-server.jar
ENTRYPOINT java -jar ${options} /usr/local/lib/fc-service-server.jar