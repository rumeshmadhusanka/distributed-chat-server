[![Maven Build](https://github.com/rumeshmadhusanka/distributed-chat-server/actions/workflows/build.yaml/badge.svg)](https://github.com/rumeshmadhusanka/distributed-chat-server/actions/workflows/build.yaml)
## Install dependencies

```bash
sudo apt update
sudo apt install openjdk-11-jdk maven -y
```

## Build and Run

```bash
mvn clean install
java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s1 config 
```

## Run with Remote Debugging activated

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8001 -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s1 config
```
