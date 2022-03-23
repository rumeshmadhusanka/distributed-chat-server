[![Maven Build](https://github.com/rumeshmadhusanka/distributed-chat-server/actions/workflows/build.yaml/badge.svg)](https://github.com/rumeshmadhusanka/distributed-chat-server/actions/workflows/build.yaml)
## Install dependencies
This application requires 
- JDK 11 or higher.
- Maven 3.6.3 or higher

```bash
sudo apt update
sudo apt install openjdk-11-jdk maven -y
```

## Build

```bash
mvn clean install
```
## Run
```
java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar <server-id> <config-file-name> 
```

Ex:
```
java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s1 config 
```

## Run with Remote Debugging

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8001 -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s1 config
```
