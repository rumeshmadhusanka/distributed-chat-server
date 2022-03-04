#!/usr/bin/bash
trap "kill 0" EXIT
mvn clean install
java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s1 conf &
java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s2 conf &
java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s3 conf &
wait