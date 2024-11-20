FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/yappysearchservice-0.0.1-SNAPSHOT.jar /app/yappysearchservice.jar

COPY src/main/resources/ok.json /app/src/main/resources/ok.json

CMD ["java", "-jar", "yappysearchservice.jar"]