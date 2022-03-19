FROM maven:3.8.4-eclipse-temurin-11 AS build-image
WORKDIR /app
COPY ./pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./src ./src
RUN mvn package


FROM openjdk:11-jre AS runtime-image
COPY --from=build-image /app/target/ChatServer-1.0.0-jar-with-dependencies.jar server.jar
VOLUME ./conf:/conf
CMD "java -jar server.jar s1 conf"

# Build the docker image: docker build -t  chat-server .
# Run: docker run -v ${PWD}/conf:/conf -p 44444:44444  chat-server java -jar server.jar s1 conf