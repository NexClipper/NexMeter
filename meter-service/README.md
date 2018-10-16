## Run Meter service

```
docker run -p 8080:8080\
 -e SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=127.0.0.1:9092\
 -e SPRING_REDIS=localhost:6379\
 -e SPRING_INFLUXDB_URL=http://localhost:8086\
 -e SPRING_INFLUXDB_USERNAME=admin\
 -e SPRING_INFLUXDB_PASSWORD=password\
 -e SPRING_INFLUXDB_DATABASE=meter\
 -e SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/killbill\
 -e SPRING_DATASOURCE_USERNAME=root\
 -e SPRING_DATASOURCE_PASSWORD=killbill\
 -e KILLBILL_URL=http://localhost:9090\
 -e KILLBILL_USERNAME=admin\
 -e KILLBILL_PASSWORD=password\
 -e KILLBILL_APIKEY=admin\
 -e KILLBILL_APISECRET=password\
 sppark/meter:v1
 
```

as yml

```
version: '3.3'

services:
  redis:
    image: redis:latest
    container_name: redis
    ports:
      - "6379:6379"

  kafka:
    image: wurstmeister/kafka
    container_name: kafka-reactive-processor
    ports:
      - "9092:9092"
    environment:
#      - KAFKA_ADVERTISED_HOST_NAME=127.0.0.1
      - KAFKA_ADVERTISED_HOST_NAME=kafka
      - KAFKA_ADVERTISED_PORT=9092
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
    depends_on:
      - zookeeper

  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"
    environment:
      - KAFKA_ADVERTISED_HOST_NAME=zookeeper

  db:
    image: killbill/mariadb:0.20
    volumes:
      - /Users/uengine/docker/killbill:/var/lib/mysql
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=killbill

  influxdb:
    image: influxdb
    volumes:
      - /Users/uengine/docker/influxdb:/var/lib/influxdb
    ports:
      - "8086:8086"
    environment:
      - INFLUXDB_DB=meter
      - INFLUXDB_ADMIN_ENABLED=true
      - INFLUXDB_ADMIN_USER=admin
      - INFLUXDB_ADMIN_PASSWORD=password

#  killbill:
#    image: sppark/killbill:0.20.2
#    ports:
#      - "8090:8080"
#    environment:
#      - KILLBILL_DAO_URL=jdbc:mysql://db:3306/killbill
#      - KILLBILL_DAO_USER=root
#      - KILLBILL_DAO_PASSWORD=killbill
#      - TEST_MODE=true

#  kaui:
#    image: killbill/kaui:1.0.4
#    ports:
#      - "9090:8080"
#    environment:
#      - KAUI_CONFIG_DAO_URL=jdbc:mysql://db:3306/kaui
#      - KAUI_CONFIG_DAO_USER=root
#      - KAUI_CONFIG_DAO_PASSWORD=killbill
#      - KAUI_KILLBILL_URL=http://killbill:8090

  meter-service:
    depends_on:
      - kafka
      - db
      - redis
      - influxdb
    image: sppark/meter:v1
    ports:
      - "8080:8080"
    restart: always
    environment:
      SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: kafka:9092
      SPRING_REDIS: redis:6379
      SPRING_INFLUXDB_URL: http://influxdb:8086
      SPRING_INFLUXDB_USERNAME: admin
      SPRING_INFLUXDB_PASSWORD: password
      SPRING_INFLUXDB_DATABASE: meter
      SPRING_DATASOURCE_URL: jdbc:mariadb://db:3306/killbill
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: killbill
      KILLBILL_URL: http://172.30.1.4:9090
      KILLBILL_USERNAME: admin
      KILLBILL_PASSWORD: password
      KILLBILL_APIKEY: admin
      KILLBILL_APISECRET: password

volumes:
  data-volume:
```