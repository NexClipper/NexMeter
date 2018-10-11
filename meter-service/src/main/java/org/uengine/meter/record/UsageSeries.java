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

    private String unit;
    private Unit.Rule rule;
    private List<Object[]> series;

    public void applyInfluxSeries(QueryResult.Result queryResult) throws Exception {

        DecimalFormat df = new DecimalFormat("#.###");
        this.series = new ArrayList<>();

        if (queryResult == null || queryResult.getSeries() == null) {
            return;
        }
        final QueryResult.Series series = queryResult.getSeries().get(0);
        final List<List<Object>> values = series.getValues();

        //putEmptyPeriod: Putting the amount value in an empty period
        int maxIndex = 0;
        boolean usePutEmptyPeriod = false;
        double previousAmount = 0;

        //if avg or peak, and putEmptyPeriod
        if ((Unit.Rule.CountingMethod.AVG.equals(rule.getCountingMethod()) ||
                Unit.Rule.CountingMethod.PEAK.equals(rule.getCountingMethod())) && rule.isPutEmptyPeriod()) {

            usePutEmptyPeriod = true;

            //max index which amount is not null.
            maxIndex = IntStream.range(0, values.size())
                    .filter(i -> values.get(i).get(1) != null).max().getAsInt();
        }

        for (int i = 0; i < values.size(); i++) {
            final List<Object> value = values.get(i);
            final long time = TimeUtil.fromInfluxDBTimeFormat((String) value.get(0));
            if (value.get(1) == null) {
                //if index is smaller than maxIndex, fill previous amount.
                if (usePutEmptyPeriod && i < maxIndex) {
                    this.series.add(new Object[]{time, previousAmount});
                }
                //else, fill zero
                else {
                    this.series.add(new Long[]{time, 0L});
                }

            } else {
                final double val = Double.valueOf(df.format(value.get(1)));
                this.series.add(new Object[]{time, val});
                previousAmount = val;
            }

        }
        for (List<Object> value : values) {
            final long time = TimeUtil.fromInfluxDBTimeFormat((String) value.get(0));
            if (value.get(1) == null) {
                this.series.add(new Object[]{time, 0L});
            } else {
                this.series.add(new Object[]{time, Double.valueOf(df.format(value.get(1)))});
            }
        }
    }
}
