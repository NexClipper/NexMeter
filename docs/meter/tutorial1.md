# Prepare

## run killbill,db,kaui docker

In order to import UsageItems from the metering server, 
I have made some modifications to the `org.killbill.billing.invoice.generator.UsageInvoiceItemGenerator.java` class. 
If possible, I would like to ask the creator of Killbill to be able to move it with the plugin function.

First, use `sppark/killbill:0.20.2` for the modifications codes.

```
# Killbill
docker run -p 8090:8080\
 -e KILLBILL_DAO_URL=jdbc:mysql://<docker-machine-ip>:3306/killbill\
 -e KILLBILL_DAO_USER=root\
 -e KILLBILL_DAO_PASSWORD=killbill\
 -e TEST_MODE=true\
 sppark/killbill:0.20.2
 

# KAUI
docker run -p 9090:8080\
 -e KAUI_CONFIG_DAO_URL=jdbc:mysql://<docker-machine-ip>:3306/kaui\
 -e KAUI_CONFIG_DAO_USER=root\
 -e KAUI_CONFIG_DAO_PASSWORD=killbill\
 -e KAUI_KILLBILL_URL=http://<docker-machine-ip>:8090\
 killbill/kaui:1.0.4
 
  
# Database
docker run -p 3306:3306\
 -v <host-db-mount-path>:/var/lib/mysql\
 -e MYSQL_ROOT_PASSWORD=killbill\
 killbill/mariadb:0.20 
```

## After database docker launched, update email notification ddl

```
DROP table If exists email_notifications_configuration;
CREATE TABLE email_notifications_configuration (
  record_id serial unique,
  kb_account_id varchar(255) NOT NULL,
  kb_tenant_id varchar(255) NOT NULL,
  event_type varchar(255) NOT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY (record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
CREATE UNIQUE INDEX email_notifications_configuration_event_type_kb_account_id ON email_notifications_configuration(event_type, kb_account_id);
CREATE INDEX email_notifications_configuration_kb_account_id ON email_notifications_configuration(kb_account_id);
CREATE INDEX email_notifications_configuration_kb_tenant_id ON email_notifications_configuration(kb_tenant_id);
CREATE INDEX email_notifications_configuration_event_type_kb_tenant_id ON email_notifications_configuration(event_type, kb_tenant_id);
```

## Create first tenant

1. Visit `http://<KAUI URL>`

2. login as `admin/password`

3. Create tenant as **apiKey:admin**, **apiSecret:password**


## run metering compose file

The metering service ddl will be automatically created in the killbill database.

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
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
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

  meter-service:
    depends_on:
      - kafka
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
      SPRING_DATASOURCE_URL: jdbc:mariadb://<killbill-db-host>:3306/killbill
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: killbill
      KILLBILL_URL: http://<killbill-url>
      KILLBILL_USERNAME: admin
      KILLBILL_PASSWORD: password
      KILLBILL_APIKEY: admin
      KILLBILL_APISECRET: password
```


## notification url and tenant config

Create killbill notification url and tenant config

```
POST /1.0/kb/tenants/uploadPerTenantConfig
{
 "org.killbill.meter.url":"http://<metering-url>/meter/record/usageItems"
}


POST /1.0/kb/tenants/registerNotificationCallback?cb=http://<metering-url>/meter/billing/event
```

## upload sample catalog for tenant

```
<catalog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:noNamespaceSchemaLocation="CatalogSchema.xsd ">

    <effectiveDate>2018-10-08T00:00:00+00:00</effectiveDate>
    <catalogName>NexCloud</catalogName>

    <recurringBillingMode>IN_ADVANCE</recurringBillingMode>

    <currencies>
        <currency>USD</currency>
    </currencies>

    <units>
        <unit name="host"/>
        <unit name="analytics"/>
    </units>

    <products>
        <product name="NexCloud">
            <category>BASE</category>
        </product>
    </products>

    <rules>
        <changePolicy>
            <changePolicyCase>
                <policy>IMMEDIATE</policy>
            </changePolicyCase>
        </changePolicy>
        <changeAlignment>
            <changeAlignmentCase>
                <alignment>START_OF_SUBSCRIPTION</alignment>
            </changeAlignmentCase>
        </changeAlignment>
        <cancelPolicy>
            <cancelPolicyCase>
                <policy>IMMEDIATE</policy>
            </cancelPolicyCase>
        </cancelPolicy>
        <createAlignment>
            <createAlignmentCase>
                <alignment>START_OF_BUNDLE</alignment>
            </createAlignmentCase>
        </createAlignment>
        <billingAlignment>
            <billingAlignmentCase>
                <productCategory>ADD_ON</productCategory>
                <alignment>BUNDLE</alignment>
            </billingAlignmentCase>
            <billingAlignmentCase>
                <alignment>ACCOUNT</alignment>
            </billingAlignmentCase>
        </billingAlignment>
    </rules>

    <plans>
        <plan name="standard-monthly">
            <product>NexCloud</product>
            <finalPhase type="EVERGREEN">
                <duration>
                    <unit>UNLIMITED</unit>
                </duration>
                <usages>
                    <usage name="usage-monthly-in-standard" billingMode="IN_ARREAR" usageType="CONSUMABLE">
                        <billingPeriod>MONTHLY</billingPeriod>
                        <tiers>
                            <tier>
                                <blocks>
                                    <tieredBlock>
                                        <unit>host</unit>
                                        <size>1</size>
                                        <prices>
                                            <price>
                                                <currency>USD</currency>
                                                <value>0.18</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                    <tieredBlock>
                                        <unit>analytics</unit>
                                        <size>100</size>
                                        <prices>
                                            <price>
                                                <currency>USD</currency>
                                                <value>2</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                </blocks>
                            </tier>
                        </tiers>
                    </usage>
                </usages>
            </finalPhase>
        </plan>
        <plan name="pro-monthly">
            <product>NexCloud</product>
            <finalPhase type="EVERGREEN">
                <duration>
                    <unit>UNLIMITED</unit>
                </duration>
                <usages>
                    <usage name="usage-monthly-in-pro" billingMode="IN_ARREAR" usageType="CONSUMABLE">
                        <billingPeriod>MONTHLY</billingPeriod>
                        <tiers>
                            <tier>
                                <blocks>
                                    <tieredBlock>
                                        <unit>host</unit>
                                        <size>1</size>
                                        <prices>
                                            <price>
                                                <currency>USD</currency>
                                                <value>28</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                    <tieredBlock>
                                        <unit>analytics</unit>
                                        <size>200</size>
                                        <prices>
                                            <price>
                                                <currency>USD</currency>
                                                <value>2</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                </blocks>
                            </tier>
                        </tiers>
                    </usage>
                </usages>
            </finalPhase>
        </plan>
    </plans>
    <priceLists>
        <defaultPriceList name="DEFAULT">
            <plans>
                <plan>standard-monthly</plan>
                <plan>pro-monthly</plan>
            </plans>
        </defaultPriceList>
    </priceLists>
</catalog>
```











