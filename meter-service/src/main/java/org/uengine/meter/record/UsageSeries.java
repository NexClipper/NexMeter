package org.uengine.meter.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.TimeUtil;
import org.uengine.meter.rule.Unit;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;

@Data
public class UsageSeries {

    private Unit unitRule;
    private List<Object[]> series;

    public void applyInfluxSeries(QueryResult.Result queryResult) throws Exception {

        DecimalFormat df = new DecimalFormat("#.###");
        this.series = new ArrayList<>();

        if (queryResult == null || queryResult.getSeries() == null) {
            return;
        }
        final QueryResult.Series series = queryResult.getSeries().get(0);
        final List<List<Object>> values = series.getValues();

        for (int i = 0; i < values.size(); i++) {
            final List<Object> value = values.get(i);
            final long time = TimeUtil.fromInfluxDBTimeFormat((String) value.get(0));
            if (value.get(1) == null) {
                this.series.add(new Long[]{time, 0L});

            } else {
                final double val = Double.valueOf(df.format(value.get(1)));
                this.series.add(new Object[]{time, val});
            }
        }
    }
}
