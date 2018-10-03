package org.uengine.meter.influxdb;

import lombok.Data;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "spring.influxdb")
@Data
public class InfluxConfig {

    private String url;
    private String username;
    private String password;
    private String database;

    @Bean
    public InfluxDB influxDBTemplate() {
        InfluxDB influxDB = InfluxDBFactory.connect(url, username, password);
        influxDB.setDatabase(database);

        //recreate retentionPolicy
        String rpName = "meterRetentionPolicy";
        influxDB.dropRetentionPolicy(rpName, database);
        influxDB.createRetentionPolicy(rpName, database, "180d", 1, true);
        influxDB.setRetentionPolicy(rpName);

        //enable batch flush.
        influxDB.enableBatch(BatchOptions.DEFAULTS.actions(2000).flushDuration(100));
        return influxDB;
    }
}
