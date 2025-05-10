# lettuce-test


### Redis Setup

```shell
docker-componse -f redis-replica.yaml up 
```
- this will start 3 containers for redis master on 6379
- slave1 - 6479
- slave2 - 6579

### Build
```shell
./gradlew :bootJar
```

### Run the program
```shell
java -jar build/libs/lettuce-bug-1.0-SNAPSHOT.jar
```

### Testing program
- Set dummy data by running curl
```shell
curl --location --request POST 'http://localhost:8080/test-redis'
```
- get the dummy data by running
```shell
curl http://localhost:8080/test-redis
```

### Possible Bug
- Bring down redis master
```shell
docker stop redis-master
```
- Start the application
- Hit the curl
- bring the master back up
```shell
docker start redis-master
```
- this should make it so that redis master is now up and accessible, but the app will continue to fail

### Possible fix

```java
    final LettuceConnectionFactory factory = new LettuceConnectionFactory(rConfig, config);
//    set shareNativeConnection to false, but this means all operations will now be 1/socket, 
//    instead of being multiplexed over a single connection
factory.setShareNativeConnection(false);
    factory.afterPropertiesSet();
```