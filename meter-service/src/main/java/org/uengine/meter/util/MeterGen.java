package org.uengine.meter.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;

import java.util.*;

public class MeterGen {


    public static void main(String[] args) {
        Map requiredHeaders = new HashMap();
        requiredHeaders.put("Content-Type", "application/json");
        final ObjectMapper objectMapper = new ObjectMapper();

        final long monthHours = 24L * 30L;
        long current = new Date().getTime() - (monthHours * 3600L * 1000L);

        for (int i = 0; i < monthHours; i++) {
            current = current + (3600L * 1000L);

            final ArrayList<Map> records = new ArrayList<>();
//            Map analytics = new HashMap();
//            analytics.put("unit", "analytics");
//            analytics.put("user", "tester@gmail.com");
//            analytics.put("amount", (long) (Math.random() * 50 + 1));
//            analytics.put("time", current);
//            records.add(analytics);

            Map host = new HashMap();
            host.put("unit", "host");
            host.put("user", "tester@gmail.com");
            host.put("amount", (long) (Math.random() * 25 + 1));
            host.put("time", current);
            records.add(host);

            try {
                final String value = objectMapper.writeValueAsString(records);
                String url = "http://localhost:8080/meter/record/json";
                HttpUtils httpUtils = new HttpUtils();
                HttpResponse httpResponse = httpUtils.makeRequest("POST", url, value, requiredHeaders);
            } catch (Exception ex) {

            }
        }

    }

}
