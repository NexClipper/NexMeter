package org.uengine.meter.record;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.uengine.meter.rule.Unit;

import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

    public void findByUnitAndUserAndSubscriptionId(
            Unit.Rule.CountingMethod countingMethod,
            String unit,
            String user,
            String subscriptionId,
            Long start,
            Long end,
            String division) {

        String function = null;
        if (Unit.Rule.CountingMethod.AVG.equals(countingMethod)) {
            function = "mean";
        } else if (Unit.Rule.CountingMethod.PEAK.equals(countingMethod)) {
            function = "max";
        } else if (Unit.Rule.CountingMethod.SUM.equals(countingMethod)) {
            function = "sum";
        }

        String subId = null;
        if (subscriptionId == null) {
            subId = "";
        } else {
            subId = subscriptionId;
        }

        final StringJoiner joiner = new StringJoiner(" ");
        joiner
                .add("select")
                .add(function + "(\"amount\")")
                .add("from record where")
                .add("time < " + end + "ms")
                .add("and")
                .add("time > " + start + "ms")
                .add("and")
                .add("unit='" + unit + "'")
                .add("and")
                .add("user='" + user + "'")
                .add("and")
                .add("subscriptionId='" + subId + "'")
                .add("group by time(" + division + ")");

        final String query = joiner.toString();

        final QueryResult result = influxDBTemplate.query(new Query(query, "meter"));

        System.out.println(123);
    }
}
