package org.uengine.meter.record;

import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import org.apache.commons.lang3.time.DateUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) {
//        GrokCompiler grokCompiler = GrokCompiler.newInstance();
//        grokCompiler.registerDefaultPatterns();
//
//        final Grok grok = grokCompiler.compile("\\[%{TIMESTAMP_ISO8601:timestamp} #%{NUMBER:number}\\]\\s+%{WORD:level}\\s--\\s+%{WORD:client}");
//
//        //final Grok grok = grokCompiler.compile("%{NUMBER:response} aa");
//
//        String log = "2018-09-24T09:57:50,0513434+0000 lvl='INFO', log='main#available_engines', th='http-bio-8080-exec-3', xff='', rId='03e8fc62-11f7-4d39-98c6-394b6a0e3b40', aId='', tId='b941c5fe-4a04-4054-8755-bef9b2b98353', I, [2018-09-24T18:57:50.050994 #25796]  INFO -- KillBillClient: Request method='GET', uri='http://127.0.0.1:18080/1.0/kb/nodesInfo'";
//
//        Match gm = grok.match(log);
//
//        /* Get the map with matches */
//        final Map<String, Object> capture = gm.capture();
//
//        //java pattern 보기
//        String text = capture.get("timestamp").toString();
//        System.out.println(text);
//
//        try {
//            final Date date = DateUtils.parseDate(text,
//                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
//                    "yyyy-MM-dd'T'HH:mm:ss",
//                    "dd.MM.yyyy HH:mm:ss",
//                    "dd.MM.yyyy");
//            System.out.println(date.toString());
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }


        //influxDb

        InfluxDB influxDB = InfluxDBFactory.connect("http://127.0.0.1:8086", "admin", "password");
        String dbName = "meter";
        influxDB.setDatabase(dbName);


        influxDB.dropRetentionPolicy("meter-rp1", dbName);
        influxDB.createRetentionPolicy("meter-rp1", dbName, "30d", 1, true);

        //BatchPoints
        BatchPoints batchPoints = BatchPoints
                .database(dbName)
                .retentionPolicy("defaultPolicy")
                .build();

        Point point1 = Point.measurement("memory")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("name", "server1")
                .addField("free", 4743656L)
                .addField("used", 1015096L)
                .addField("buffer", 1010467L)
                .build();

        Point point2 = Point.measurement("memory")
                .time(System.currentTimeMillis() - 100, TimeUnit.MILLISECONDS)
                .addField("name", "server1")
                .addField("free", 4743696L)
                .addField("used", 1016096L)
                .addField("buffer", 1008467L)
                .build();

        batchPoints.point(point1);
        batchPoints.point(point2);
        influxDB.write(batchPoints);


        final long time = System.currentTimeMillis();
        System.out.println(time);

        final Point build = Point.measurement("cpu")
                .time(time, TimeUnit.MILLISECONDS)
                .addField("idle", 90L)
                .addField("user", 9L)
                .addField("system", 1L)
                .build();
        final String s = build.toString();
        System.out.println(s);


        influxDB.write(Point.measurement("cpu")
                .time(time, TimeUnit.MILLISECONDS)
                .addField("idle", 90L)
                .addField("user", 9L)
                .addField("system", 1L)
                .build());

        influxDB.write(Point.measurement("disk")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("used", 80L)
                .addField("free", 1L)
                .build());

        Query query = new Query("SELECT idle FROM cpu", dbName);

        influxDB.query(query, queryResult -> {
            // Do something with the result...
            for (QueryResult.Result result : queryResult.getResults()) {
                System.out.println(result.toString());
            }

        }, throwable -> {
            // Do something with the error...
        });
//
////        final QueryResult result = influxDB.query(query);
////        final List<QueryResult.Result> results = result.getResults();
////        for (QueryResult.Result result1 : results) {
////            result1.getSeries();
////        }
//
//        influxDB.close();

    }
}
