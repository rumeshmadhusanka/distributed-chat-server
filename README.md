## Install dependencies
```bash
sudo apt update
sudo apt install openjdk-11-jdk maven -y
```

## Build and Run
```bash
mvn clean install
java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s1 conf 
```

