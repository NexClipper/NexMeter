package org.uengine.meter.record;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RecordInfluxRepository {

    @Autowired
    private InfluxDB influxDBTemplate;

    public Record write(Record record) {
        Point point = Point.measurement("record")
                .time(record.getTime(), TimeUnit.MILLISECONDS)
                .addField("amount", record.getAmount())
                .addField("user", record.getUser())
                .addField("unit", record.getUnit())
                .addField("basePlan", record.getBasePlan() == null ? "" : record.getBasePlan())
                .addField("addonPlan", record.getAddonPlan() == null ? "" : record.getAddonPlan())
                .addField("subscriptionId", record.getSubscriptionId() == null ? "" : record.getSubscriptionId())
                .build();
        influxDBTemplate.write(point);
        return record;
    }
}
