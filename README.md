# Build
```
mvn install
```

# Usage

```
java -jar ./target/myXenonApp-1.0-SNAPSHOT-jar-with-dependencies.jar --peerNodes=http://127.0.0.1:8001,http://127.0.0.1:8002,http://127.0.0.1:8003 --id=host-1 --port=8001
java -jar ./target/myXenonApp-1.0-SNAPSHOT-jar-with-dependencies.jar --peerNodes=http://127.0.0.1:8001,http://127.0.0.1:8002,http://127.0.0.1:8003 --id=host-2 --port=8002
java -jar ./target/myXenonApp-1.0-SNAPSHOT-jar-with-dependencies.jar --peerNodes=http://127.0.0.1:8001,http://127.0.0.1:8002,http://127.0.0.1:8003 --id=host-3 --port=8003

curl localhost:8001/core/consistent-hashing\?key=hello
```
