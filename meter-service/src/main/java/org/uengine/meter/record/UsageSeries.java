package org.uengine.meter.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.TimeUtil;
import org.springframework.util.StringUtils;
import org.uengine.meter.rule.Unit;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

//목표 output
//- unit: disk
//    usages:
//    - rule: rule
//    - subscriptionId:
//    - series: [
//    [time, amount]
//    ]
//
//    //if free period hour
//    - amount-per-hour: [
//    {
//        time: 2018-01-03:01(String)
//            usage: 100
//        free: 50
//        total: 50
//    },
//    {
//        time: 2018-01-03:02(String)
//            usage: 40
//        free: 50 -> 40 (usage 보다 클 수 없음)
//        total: 0
//    }
//     ]
//             - amount-per-day: [
//    //if sum, free is hour
//    {
//        time: 2018-01-03(String)
//            usage: 100 + 40 (전단계의 합)
//        free: 50 + 40 (전단계의 free 합)
//        total: 50 + 0 (전단계의 total 합)
//    },
//    //if sum, free is day
//    {
//        time: 2018-01-03(String)
//            usage: 100 + 40 (전단계의 합)
//        free: 50 (freeAmount, usage 보다 클 수 없음)
//        total: 90 (usage - free)
//    }
//     ]
//             - amount-per-day: [
//    //if avg, freePeriod and periodSplitting hour
//    {
//        time: 2018-01-03(String)
//            usage: 100 + 40 (전단계의 합)
//        free: 50 + 40 (전단계의 free 합)
//        total: 50 + 0 (전단계의 total 합)
//    },
//    //if avg, freePeriod and periodSplitting day
//    {
//        time: 2018-01-03(String)
//            usage: (100 + 40)/2 (전단계의 평균/피크)
//        free: 50 (freeAmount, usage 보다 클 수 없음)
//        total: 90 (usage - free)
//    }
//     ]
//    - amount-total:
//    start:
//    end:
//    usage: amount-per-day 의 usage 총합
//    free: amount-per-day 의 free 총합
//    total: amount-per-day 의 total 총합
@Data
public class UsageSeries {

    private String unit;
    private List<Usage> usages;

    public List<Map> applyKBUsageItems(String user) {
        //add only subscriptionId exist.
        final ArrayList<Map> list = new ArrayList<>();
        if (this.usages == null || this.usages.isEmpty()) {
            return list;
        }
        for (Usage usage : this.usages) {
            final String subscriptionId = usage.getSubscriptionId();
            if (StringUtils.isEmpty(subscriptionId)) {
                continue;
            }
            final List<Item> amountPerDay = usage.amountPerDay;
            for (int i = 0; i < amountPerDay.size(); i++) {
                final Item item = amountPerDay.get(i);
                if (item.getTotal() > 0L) {
                    HashMap<Object, Object> map = new HashMap<>();
                    map.put("subscriptionId", subscriptionId);
                    map.put("unitType", this.unit);
                    map.put("date", item.getFormatted());
                    map.put("amount", item.getTotal());
                    list.add(map);
                }
            }
        }
        return list;
    }

    @Data
    @NoArgsConstructor
    public static class Usage {

        private Unit.Rule rule;
        private String subscriptionId;
        private List<Long[]> series;
        private List<Item> amountPerHour;
        private List<Item> amountPerDay;
        private AmountTotal amountTotal;

        public void applyInfluxSeries(QueryResult.Result queryResult) throws Exception {
            this.series = new ArrayList<>();

            if (queryResult == null || queryResult.getSeries() == null) {
                return;
            }
            final QueryResult.Series series = queryResult.getSeries().get(0);
            final List<List<Object>> values = series.getValues();
            for (List<Object> value : values) {
                final long time = TimeUtil.fromInfluxDBTimeFormat((String) value.get(0));
                if (value.get(1) == null) {
                    this.series.add(new Long[]{time, 0L});
                } else {
                    this.series.add(new Long[]{time, ((Double) value.get(1)).longValue()});
                }
            }
        }

        @JsonIgnore
        public static final long HOUR = 3600 * 1000;

        @JsonIgnore
        public static final long DAY = 3600 * 1000 * 24;


        // |  1   1  |   1   |       |   1  1  |  1 (add amount. it is last, add item. it is over, do next.
        //TODO 마지막 시간대를 계산하지 않고 있음.

        public void applyHour() throws Exception {
            this.amountPerHour = new ArrayList<>();

            //get yyyy-MM-dd HH starting hour time
            final Long first = this.series.get(0)[0];
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
            final String format = dateFormat.format(new Date(first));
            final Date parsed = dateFormat.parse(format);
            final long startTime = parsed.getTime();

            //set current and next
            Long current = startTime;
            Long next = current + HOUR;

            //set amountList
            List<Long> amountList = new ArrayList<Long>();

            for (int i = 0; i < this.series.size(); i++) {


                final Long[] data = this.series.get(i);

                //1. if time is less than next, add.
                if (data[0] < next) {
                    amountList.add(data[1]);
                }

                //if time is bigger than next, add new Item.
                //then, reset current,next,count,amount
                //then, do 1 again.
                else {
                    this.addHourItem(rule, amountList, current);

                    current = next;
                    next = current + HOUR;
                    amountList = new ArrayList<>();
                    i--;
                }
            }
            //finally, add item for last time series
            this.addHourItem(rule, amountList, current);
        }

        public void applyDay() throws Exception {
            this.amountPerDay = new ArrayList<>();

            //get yyyy-MM-dd starting hour time
            final Long first = this.series.get(0)[0];
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            final String format = dateFormat.format(new Date(first));
            final Date parsed = dateFormat.parse(format);
            final long startTime = parsed.getTime();

            //set current and next
            Long current = startTime;
            Long next = current + DAY;

            //set amountList
            List<Item> amountList = new ArrayList<Item>();

            for (int i = 0; i < this.amountPerHour.size(); i++) {

                final Item item = this.amountPerHour.get(i);

                //1. if time is less than next, add.
                if (item.getTime() < next) {
                    amountList.add(item);
                }

                //if time is bigger than next, add new Item.
                //then, reset current,next,count,amount
                //then, do 1 again.
                else {
                    this.addDayItem(rule, amountList, current);

                    current = next;
                    next = current + DAY;
                    amountList = new ArrayList<>();
                    i--;
                }
            }
            //finally, add item for last time series
            this.addDayItem(rule, amountList, current);
        }

        public void addHourItem(Unit.Rule rule, List<Long> amountList, Long current) {

            long itemUsage = 0L;
            if (!amountList.isEmpty()) {
                if (Unit.Rule.CountingMethod.SUM.equals(rule.getCountingMethod())) {
                    itemUsage = amountList.stream().mapToLong(v -> v).sum();
                }
                if (Unit.Rule.CountingMethod.AVG.equals(rule.getCountingMethod())) {
                    itemUsage = (long) Math.ceil(
                            new Double(amountList.stream().mapToLong(v -> v).sum()) / new Double(amountList.size()));
                }
                if (Unit.Rule.CountingMethod.PEAK.equals(rule.getCountingMethod())) {
                    final long max = amountList.stream().mapToLong(v -> v)
                            .max()
                            .getAsLong();
                    itemUsage = max;
                }
            }

            //if free period is hour
            long itemFree = 0L;
            if (Unit.Rule.FreePeriod.HOUR.equals(rule.getFreePeriod())) {
                itemFree = rule.getFreeAmount();
            }

            //create item
            final Item item = new Item(current, itemUsage, itemFree);
            item.setFormatted(new SimpleDateFormat("yyyy-MM-dd HH").format(current));
            amountPerHour.add(item);
        }

        public void addDayItem(Unit.Rule rule, List<Item> amountList, Long current) {
            long itemUsage = 0L;
            long itemFree = 0L;
            if (!amountList.isEmpty()) {
                if (Unit.Rule.CountingMethod.SUM.equals(rule.getCountingMethod())) {

                    itemUsage = amountList.stream().map(item -> item.getUsage()).mapToLong(v -> v).sum();

                    if (Unit.Rule.FreePeriod.HOUR.equals(rule.getFreePeriod())) {
                        itemFree = amountList.stream().map(item -> item.getFree()).mapToLong(v -> v).sum();

                    } else if (Unit.Rule.FreePeriod.DAY.equals(rule.getFreePeriod())) {
                        itemFree = rule.getFreeAmount();

                    } else {
                        itemFree = 0L;
                    }
                }

                if (Unit.Rule.CountingMethod.AVG.equals(rule.getCountingMethod()) ||
                        Unit.Rule.CountingMethod.PEAK.equals(rule.getCountingMethod())) {

                    //if avg or peak, freePeriod and periodSplitting hour
                    if (Unit.Rule.PeriodSplitting.HOUR.equals(rule.getPeriodSplitting())) {

                        itemUsage = amountList.stream().map(item -> item.getUsage()).mapToLong(v -> v).sum();

                        if (Unit.Rule.FreePeriod.HOUR.equals(rule.getFreePeriod())) {
                            itemFree = amountList.stream().map(item -> item.getFree()).mapToLong(v -> v).sum();
                        } else {
                            itemFree = 0L;
                        }

                    }
                    //if avg, freePeriod and periodSplitting day
                    else if (Unit.Rule.PeriodSplitting.DAY.equals(rule.getPeriodSplitting())) {
                        if (Unit.Rule.CountingMethod.AVG.equals(rule.getCountingMethod())) {

                            itemUsage = (long) Math.ceil(
                                    new Double(amountList.stream()
                                            .map(item -> item.getUsage())
                                            .mapToLong(v -> v)
                                            .sum())
                                            / new Double(amountList.size()));
                        }

                        if (Unit.Rule.CountingMethod.PEAK.equals(rule.getCountingMethod())) {
                            final long max =
                                    amountList.stream().map(item -> item.getUsage())
                                            .mapToLong(v -> v).max().getAsLong();
                            itemUsage = max;
                        }

                        if (Unit.Rule.FreePeriod.DAY.equals(rule.getFreePeriod())) {
                            itemFree = rule.getFreeAmount();
                        } else {
                            itemFree = 0L;
                        }
                    }
                }
            }

            //create item
            final Item item = new Item(current, itemUsage, itemFree);
            item.setFormatted(new SimpleDateFormat("yyyy-MM-dd").format(current));
            amountPerDay.add(item);
        }

        public void applyTotal() {
            this.amountTotal = new AmountTotal();
            if (!this.amountPerDay.isEmpty()) {
                final int days = this.amountPerDay.size();
                final Long start = this.amountPerDay.get(0).getTime();
                final Long end = this.amountPerDay.get(days - 1).getTime();

                this.amountTotal.setDays(days);
                this.amountTotal.setStart(new Date(start));
                this.amountTotal.setEnd(new Date(end));

                this.amountTotal.setUsage(
                        this.amountPerDay.stream().map(item -> item.getUsage()).mapToLong(v -> v).sum());

                this.amountTotal.setFree(
                        this.amountPerDay.stream().map(item -> item.getFree()).mapToLong(v -> v).sum());

                this.amountTotal.setTotal(
                        this.amountPerDay.stream().map(item -> item.getTotal()).mapToLong(v -> v).sum());
            }
        }
    }


    @Data
    @NoArgsConstructor
    public static class AmountTotal {
        private Date start;
        private Date end;
        private int days;
        private Long usage;
        private Long free;
        private Long total;
    }

    @Data
    public static class Item {
        private Long time;
        private Long usage;
        private Long free;
        private Long total;
        private String formatted;

        public Item() {
        }

        public Item(Long time, Long usage, Long free) {
            this.time = time;
            this.usage = usage;
            this.free = free;

            //free can not bigger than usage
            if (free > usage) {
                this.free = usage;
            }
            this.total = this.usage - this.free;
        }
    }
}
