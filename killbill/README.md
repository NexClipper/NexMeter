# Manual install

## kpm install

```
$ gem install kpm
$ mkdir killbill
$ cd killbill
$ kpm install
```

# show plugins

```
$ kpm info
```

or [https://github.com/killbill/killbill-cloud/blob/master/kpm/lib/kpm/plugins_directory.yml](https://github.com/killbill/killbill-cloud/blob/master/kpm/lib/kpm/plugins_directory.yml)

# install plugins

```
kpm install_java_plugin email-notifications
kpm install_java_plugin analytics
kpm install_java_plugin payment-retries
kpm install_ruby_plugin payment-test
kpm install_ruby_plugin paypal
kpm install_ruby_plugin stripe
```

# Docker

## build killbill

```
cd <killbill path> && mvn install
mv <target-path>/killbill-profiles-killbill-0.20.2.war /killbill
```

다음과 같이 압축 후 /killbill 폴더로 이동시킬 것

```
ruby
├── killbill-paypal-express
├── killbill-stripe
└── ruby_plugins.zip

java
├── analytics-plugin
├── java_plugins.zip
├── killbill-email-notifications-plugin
└── payment-retries-plugin
``` 

## build docker

```
docker build -t sppark/killbill:0.20.2 ./
```

## run docker

```
# Killbill
docker run -p 8080:8080\
 -e KILLBILL_DAO_URL=jdbc:mysql://192.168.43.74:3306/killbill\
 -e KILLBILL_DAO_USER=root\
 -e KILLBILL_DAO_PASSWORD=killbill\
 -e TEST_MODE=true\
 sppark/killbill:0.20.2
 

# KAUI
docker run -p 9090:8080\
 -e KAUI_CONFIG_DAO_URL=jdbc:mysql://172.30.1.4:3306/kaui\
 -e KAUI_CONFIG_DAO_USER=root\
 -e KAUI_CONFIG_DAO_PASSWORD=killbill\
 -e KAUI_KILLBILL_URL=http://172.30.1.4:8080\
 killbill/kaui:1.0.4
 
docker run -p 9090:8080\
 -e KAUI_CONFIG_DAO_URL=jdbc:mysql://121.167.146.57:12001/kaui\
 -e KAUI_CONFIG_DAO_USER=root\
 -e KAUI_CONFIG_DAO_PASSWORD=killbill\
 -e KAUI_KILLBILL_URL=http://121.167.146.57:12002\
 killbill/kaui:1.0.4 
 
 
# Database
docker run -p 3306:3306\
 -v /Users/uengine/docker/killbill:/var/lib/mysql\
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

